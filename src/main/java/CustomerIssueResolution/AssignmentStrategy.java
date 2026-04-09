package CustomerIssueResolution;

import java.util.Comparator;

/**
 * Strategy interface for agent selection algorithms.
 * Implement this to add new assignment strategies without modifying Solution.
 * The caller applies a stable sort using this comparator — ties are resolved by
 * insertion order (first added agent wins), matching the spec's "any one of them".
 */
public interface AssignmentStrategy {
    /**
     * Returns a Comparator that orders agents from most preferred (first/lowest)
     * to least preferred, for the given issueType.
     */
    Comparator<Agent> getComparator(int issueType);
}
