package JobScheduler;

import java.util.Comparator;

/**
 * Strategy interface for machine selection algorithms.
 * Implement this to add new assignment criteria.
 * The tiebreaker (lexicographically smallest machineId) is applied automatically
 * by the scheduler after this comparator.
 */
public interface AssignmentCriteria {
    /**
     * Returns a Comparator that orders machines from most preferred (first) to least preferred.
     */
    Comparator<Machine> getComparator();
}
