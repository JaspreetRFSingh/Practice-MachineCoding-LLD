package CustomerIssueResolution;

import java.util.Comparator;

/**
 * Strategy 2: Assign the agent with the fewest open issues of this specific type.
 * Spreads type-specific load evenly — used for training agents on a particular issue type.
 */
public class LeastOpenByTypeStrategy implements AssignmentStrategy {
    @Override
    public Comparator<Agent> getComparator(int issueType) {
        return Comparator.comparingInt(a -> a.openByType.getOrDefault(issueType, 0));
    }
}
