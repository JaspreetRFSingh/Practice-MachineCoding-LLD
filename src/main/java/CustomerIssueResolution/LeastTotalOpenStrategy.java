package CustomerIssueResolution;

import java.util.Comparator;

/**
 * Strategy 0: Assign the agent with the lowest total number of open issues.
 * Optimises for balanced load across all agents.
 */
public class LeastTotalOpenStrategy implements AssignmentStrategy {
    @Override
    public Comparator<Agent> getComparator(int issueType) {
        return Comparator.comparingInt(a -> a.totalOpenIssues);
    }
}
