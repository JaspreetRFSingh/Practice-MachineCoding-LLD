package splitwise;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SplitBook {

    // userId → displayName; putIfAbsent ensures safe concurrent registration
    private final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();

    // Prevents duplicate expense processing; ConcurrentHashMap.newKeySet() gives a thread-safe Set
    private final Set<Integer> expenseIds = ConcurrentHashMap.newKeySet();

    // userId → net balance: positive = owed to user, negative = user owes
    // BigDecimal avoids floating-point drift across many expenses
    private final ConcurrentHashMap<String, BigDecimal> netBalance = new ConcurrentHashMap<>();

    // Write lock: guards multi-key balance update in recordExpense (all-or-nothing)
    // Read lock: takes a consistent snapshot in listBalances
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void registerUser(String userId, String displayName) {
        // putIfAbsent is atomic — idempotent under concurrent calls
        if (users.putIfAbsent(userId, displayName) == null)
            netBalance.put(userId, BigDecimal.ZERO);
    }

    public void recordExpense(int expenseId, List<String> members, List<Integer> paid) {
        // expenseIds.add is atomic — exactly one thread wins for a given expenseId
        if (!expenseIds.add(expenseId)) return;

        for (String m : members) {
            if (!users.containsKey(m)) return;
        }

        int n = members.size();
        BigDecimal total = BigDecimal.ZERO;
        for (int p : paid) total = total.add(BigDecimal.valueOf(p));

        // Fair share per person, rounded HALF_UP to 2 decimal places
        BigDecimal share = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        // First member absorbs the rounding remainder so sum(shares) == total exactly
        BigDecimal firstShare = total.subtract(share.multiply(BigDecimal.valueOf(n - 1)));

        rwLock.writeLock().lock();
        try {
            for (int i = 0; i < n; i++) {
                BigDecimal paidAmt = BigDecimal.valueOf(paid.get(i));
                BigDecimal myShare = (i == 0) ? firstShare : share;
                // delta > 0: paid more than share → owed money; delta < 0: paid less → owes money
                netBalance.merge(members.get(i), paidAmt.subtract(myShare), BigDecimal::add);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public List<String> listBalances() {
        // Consistent snapshot — no expense can be partially applied during this read
        Map<String, BigDecimal> snapshot;
        rwLock.readLock().lock();
        try {
            snapshot = new HashMap<>(netBalance);
        } finally {
            rwLock.readLock().unlock();
        }

        // Separate into debtors (owe money) and creditors (are owed money)
        // TreeMap keeps both sorted lexicographically — matches required output order
        TreeMap<String, BigDecimal> debtors   = new TreeMap<>();
        TreeMap<String, BigDecimal> creditors = new TreeMap<>();
        for (var e : snapshot.entrySet()) {
            int cmp = e.getValue().compareTo(BigDecimal.ZERO);
            if (cmp < 0) debtors.put(e.getKey(), e.getValue().negate());
            else if (cmp > 0) creditors.put(e.getKey(), e.getValue());
        }

        // Greedy matching: for each debtor (alphabetical), settle against creditors (alphabetical)
        // creditorAmts is mutable — amounts drain as debts are matched
        TreeMap<String, BigDecimal> creditorAmts = new TreeMap<>(creditors);
        List<String> result = new ArrayList<>();

        for (var debtorEntry : debtors.entrySet()) {
            String debtor = debtorEntry.getKey();
            BigDecimal remaining = debtorEntry.getValue();

            for (var credEntry : creditorAmts.entrySet()) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal available = credEntry.getValue();
                if (available.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal transfer = remaining.min(available);
                result.add(debtor + " owes " + credEntry.getKey() + ": "
                           + transfer.setScale(2, RoundingMode.HALF_UP));
                credEntry.setValue(available.subtract(transfer));
                remaining = remaining.subtract(transfer);
            }
        }

        // Entries are produced in debtor order (outer TreeMap) then creditor order (inner TreeMap)
        // Explicit sort guarantees the spec even if the above changes
        result.sort(Comparator.naturalOrder());
        return result;
    }
}
