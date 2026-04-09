package CustomerIssueResolution;

import java.util.Comparator;

/**
 * Strategy 1: Assign the agent who has resolved the most issues of this type.
 * Routes complex issues to the most experienced agent for that category.
 */
public class MostResolvedByTypeStrategy implements AssignmentStrategy {
    @Override
    public Comparator<Agent> getComparator(int issueType) {
        // Reverse order: higher resolvedByType count is preferred (comes first)
        return Comparator.comparingInt((Agent a) -> a.resolvedByType.getOrDefault(issueType, 0))
                         .reversed();
    }
}
