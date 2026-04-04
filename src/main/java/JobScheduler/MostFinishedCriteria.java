package JobScheduler;

import java.util.Comparator;

/** Criteria 1: Prefer machine with most finished jobs. */
public class MostFinishedCriteria implements AssignmentCriteria {
    @Override
    public Comparator<Machine> getComparator() {
        return Comparator.comparingInt(Machine::getFinishedCount).reversed();
    }
}
