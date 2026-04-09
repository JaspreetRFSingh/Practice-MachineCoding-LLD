package CustomerIssueResolution;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Solution {

    private List<String> issueTypes;
    private ConcurrentHashMap<String, Issue> issues;
    private ConcurrentHashMap<String, Agent> agents;
    private Helper03 helper;

    /**
     * Single lock guarding all assign and resolve operations.
     *
     * Why one lock covers both assignIssue AND resolveIssue:
     *   - assignIssue reads agent.totalOpenIssues / openByType / resolvedByType to pick the best agent.
     *   - resolveIssue writes those same counters.
     *   If both could run concurrently, an in-flight resolve could corrupt the selection snapshot.
     *   A single mutex prevents the TOCTOU race with minimal overhead (assignment is fast, not I/O-bound).
     */
    private final ReentrantLock assignLock = new ReentrantLock();

    private final Map<Integer, AssignmentStrategy> strategyRegistry = new HashMap<>();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void init(List<String> issueTypes, Helper03 helper) {
        this.issueTypes = new ArrayList<>();
        for (String type : issueTypes) {
            this.issueTypes.add(type.toLowerCase());
        }
        this.issues = new ConcurrentHashMap<>();
        this.agents = new ConcurrentHashMap<>();
        this.helper = helper;

        strategyRegistry.put(0, new LeastTotalOpenStrategy());
        strategyRegistry.put(1, new MostResolvedByTypeStrategy());
        strategyRegistry.put(2, new LeastOpenByTypeStrategy());
    }

    // -------------------------------------------------------------------------
    // Issue management
    // -------------------------------------------------------------------------

    public String createIssue(String issueId, String orderId, int issueType, String description) {
        if (issueType < 0 || issueType >= issueTypes.size()) {
            return "invalid issue type";
        }
        Issue newIssue = new Issue(issueId, orderId, issueType, description);
        Issue existing = issues.putIfAbsent(issueId, newIssue);
        return existing != null ? "issue already exists" : "issue created";
    }

    // -------------------------------------------------------------------------
    // Agent management
    // -------------------------------------------------------------------------

    public String addAgent(String agentId, List<Integer> expertise) {
        Set<Integer> expertiseSet = new HashSet<>(expertise);
        Agent newAgent = new Agent(agentId, expertiseSet);
        Agent existing = agents.putIfAbsent(agentId, newAgent);
        return existing != null ? "agent already exists" : "success";
    }

    // -------------------------------------------------------------------------
    // Assignment
    // -------------------------------------------------------------------------

    public String assignIssue(String issueId, int assignStrategy) {
        Issue issue = issues.get(issueId);
        if (issue == null) return "issue doesn't exist";

        AssignmentStrategy strategy = strategyRegistry.get(assignStrategy);

        assignLock.lock();
        try {
            if (issue.status != IssueStatus.OPEN) {
                return "issue already assigned";
            }

            int issueType = issue.issueType;

            List<Agent> eligible = agents.values().stream()
                    .filter(a -> a.expertise.contains(issueType))
                    .collect(Collectors.toList());

            if (eligible.isEmpty()) return "agent with expertise doesn't exist";

            // Sort is stable (TimSort); ties retain insertion order, so "any one" is deterministic.
            eligible.sort(strategy.getComparator(issueType));
            Agent selected = eligible.get(0);

            selected.onAssigned(issueType);
            issue.status = IssueStatus.ASSIGNED;
            issue.assignedAgentId = selected.agentId;

            return selected.agentId;
        } finally {
            assignLock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Resolution
    // -------------------------------------------------------------------------

    public void resolveIssue(String issueId, String resolution) {
        Issue issue = issues.get(issueId);
        if (issue == null) return;

        String agentId;
        String issueTypeName;

        assignLock.lock();
        try {
            if (issue.status != IssueStatus.ASSIGNED) return;

            agentId = issue.assignedAgentId;
            Agent agent = agents.get(agentId);
            if (agent == null) return;

            agent.onResolved(issueId, issue.issueType);
            issue.status = IssueStatus.RESOLVED;
            issueTypeName = issueTypes.get(issue.issueType);
        } finally {
            assignLock.unlock();
        }

        helper.print("Issue Details issueId : " + issueId
                + ", issue type index : " + issue.issueType
                + ", issue status : resolved by agent " + agentId
                + ", issue type : " + issueTypeName);
    }

    // -------------------------------------------------------------------------
    // History
    // -------------------------------------------------------------------------

    public List<String> getAgentHistory(String agentId) {
        Agent agent = agents.get(agentId);
        if (agent == null) return Collections.emptyList();
        // resolvedHistory is CopyOnWriteArrayList — safe to read without the assignLock
        return new ArrayList<>(agent.resolvedHistory);
    }
}
