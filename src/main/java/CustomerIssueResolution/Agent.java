package CustomerIssueResolution;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Agent {
    final String agentId;
    final Set<Integer> expertise;

    // All fields below are guarded by Solution.assignLock
    int totalOpenIssues;
    final Map<Integer, Integer> openByType;     // issueType -> count of open issues
    final Map<Integer, Integer> resolvedByType; // issueType -> count of resolved issues

    // CopyOnWriteArrayList: writes are under assignLock; reads (getAgentHistory) need no lock
    final List<String> resolvedHistory;

    public Agent(String agentId, Set<Integer> expertise) {
        this.agentId = agentId;
        this.expertise = expertise;
        this.totalOpenIssues = 0;
        this.openByType = new HashMap<>();
        this.resolvedByType = new HashMap<>();
        this.resolvedHistory = new CopyOnWriteArrayList<>();
    }

    public void onAssigned(int issueType) {
        totalOpenIssues++;
        openByType.merge(issueType, 1, Integer::sum);
    }

    public void onResolved(String issueId, int issueType) {
        totalOpenIssues--;
        openByType.merge(issueType, -1, Integer::sum);
        resolvedByType.merge(issueType, 1, Integer::sum);
        resolvedHistory.add(issueId);
    }
}
