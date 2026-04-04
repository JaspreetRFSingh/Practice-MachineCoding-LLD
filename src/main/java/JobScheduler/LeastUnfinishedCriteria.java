package JobScheduler;

import java.util.Comparator;

/** Criteria 0: Prefer machine with fewest unfinished jobs. */
public class LeastUnfinishedCriteria implements AssignmentCriteria {
    @Override
    public Comparator<Machine> getComparator() {
        return Comparator.comparingInt(Machine::getUnfinishedCount);
    }
}
