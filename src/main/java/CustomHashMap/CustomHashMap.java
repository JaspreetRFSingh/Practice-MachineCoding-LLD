package CustomHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom in-memory HashMap with String keys and String values.
 *
 * Collision strategy : separate chaining via a singly-linked list per bucket.
 * Rehash trigger     : checked after every put() (new key) and remove().
 *   - LF > maxLoadFactor → double bucket count until valid.
 *   - LF < minLoadFactor → halve bucket count until valid, floor = 2 buckets.
 *
 * No built-in Map, Set, or Dictionary is used internally.
 * Arrays and ArrayList are used only for bucket storage and return values.
 */
public class CustomHashMap {

    // ------------------------------------------------------------------ //
    //  Inner node — linked-list chain within a single bucket              //
    // ------------------------------------------------------------------ //

    private static class Entry {
        final String key;
        String value;
        Entry next;

        Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    // ------------------------------------------------------------------ //
    //  State                                                               //
    // ------------------------------------------------------------------ //

    private Entry[] buckets;
    private int size;
    private final double minLoadFactor;
    private final double maxLoadFactor;

    // ------------------------------------------------------------------ //
    //  Constructor                                                         //
    // ------------------------------------------------------------------ //

    @SuppressWarnings("unchecked")
    public CustomHashMap(double minLoadFactor, double maxLoadFactor) {
        this.minLoadFactor = round2(minLoadFactor);
        this.maxLoadFactor = round2(maxLoadFactor);
        this.buckets = new Entry[2];  // initial capacity = 2
        this.size = 0;
    }

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Inserts or updates the entry for key.
     * Size is incremented only on a true insertion (new key).
     * Rehash is checked only after a size-changing insertion.
     */
    public void put(String key, String value) {
        int idx = indexOf(key, buckets.length);
        Entry curr = buckets[idx];
        while (curr != null) {
            if (curr.key.equals(key)) {
                curr.value = value;  // update: LF unchanged, skip rehash check
                return;
            }
            curr = curr.next;
        }
        // Prepend new entry to bucket chain (O(1))
        Entry newEntry = new Entry(key, value);
        newEntry.next = buckets[idx];
        buckets[idx] = newEntry;
        size++;
        checkRehash();
    }

    /** Returns the value for key, or "" if absent. */
    public String get(String key) {
        Entry curr = buckets[indexOf(key, buckets.length)];
        while (curr != null) {
            if (curr.key.equals(key)) return curr.value;
            curr = curr.next;
        }
        return "";
    }

    /** Removes key and returns its value, or "" if absent. */
    public String remove(String key) {
        int idx = indexOf(key, buckets.length);
        Entry curr = buckets[idx];
        Entry prev = null;
        while (curr != null) {
            if (curr.key.equals(key)) {
                if (prev == null) buckets[idx] = curr.next;
                else prev.next = curr.next;
                size--;
                checkRehash();
                return curr.value;
            }
            prev = curr;
            curr = curr.next;
        }
        return "";
    }

    /**
     * Returns all keys in the given bucket, sorted lexicographically.
     * Returns an empty list if bucketIndex is out of range.
     */
    public List<String> getBucketKeys(int bucketIndex) {
        List<String> result = new ArrayList<>();
        if (bucketIndex < 0 || bucketIndex >= buckets.length) return result;

        // Count keys in bucket to size the array without a List internally
        int count = 0;
        Entry curr = buckets[bucketIndex];
        while (curr != null) { count++; curr = curr.next; }

        String[] keys = new String[count];
        curr = buckets[bucketIndex];
        for (int i = 0; i < count; i++, curr = curr.next) {
            keys[i] = curr.key;
        }
        Arrays.sort(keys);
        for (String k : keys) result.add(k);
        return result;
    }

    public int size() { return size; }

    public int bucketsCount() { return buckets.length; }

    // ------------------------------------------------------------------ //
    //  Hashing                                                             //
    // ------------------------------------------------------------------ //

    /**
     * hash(key) = (key.length ^ 2) + sum(charValue), a=1 .. z=26
     *
     * Always non-negative: length^2 >= 1, charValues >= 1.
     * Not cryptographic — optimised for simplicity per the problem spec.
     */
    private static int hash(String key) {
        int len = key.length();
        int charSum = 0;
        for (int i = 0; i < len; i++) {
            charSum += (key.charAt(i) - 'a' + 1);
        }
        return len * len + charSum;
    }

    private static int indexOf(String key, int bucketsCount) {
        return hash(key) % bucketsCount;
    }

    // ------------------------------------------------------------------ //
    //  Load factor & rehashing                                             //
    // ------------------------------------------------------------------ //

    private double loadFactor() {
        return round2((double) size / buckets.length);
    }

    private void checkRehash() {
        double lf = loadFactor();
        if (lf > maxLoadFactor) {
            // Grow: double until LF is within bounds
            int newCount = buckets.length * 2;
            while (round2((double) size / newCount) > maxLoadFactor) {
                newCount *= 2;
            }
            rehash(newCount);
        } else if (lf < minLoadFactor && buckets.length > 2) {
            // Shrink: halve until LF is within bounds, floor = 2
            int newCount = buckets.length / 2;
            while (newCount > 2 && round2((double) size / newCount) < minLoadFactor) {
                newCount /= 2;
            }
            rehash(newCount);
        }
    }

    /**
     * Creates a new bucket array and re-inserts every live entry.
     * Reuses existing Entry nodes (no allocation) — only rewires their next pointers.
     */
    @SuppressWarnings("unchecked")
    private void rehash(int newBucketsCount) {
        Entry[] newBuckets = new Entry[newBucketsCount];
        for (Entry head : buckets) {
            Entry curr = head;
            while (curr != null) {
                Entry next = curr.next;          // save before rewiring
                int newIdx = hash(curr.key) % newBucketsCount;
                curr.next = newBuckets[newIdx];  // prepend to new bucket
                newBuckets[newIdx] = curr;
                curr = next;
            }
        }
        buckets = newBuckets;
    }

    // ------------------------------------------------------------------ //
    //  Utility                                                             //
    // ------------------------------------------------------------------ //

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
