package CustomerIssueResolution;

public class Issue {
    final String issueId;
    final String orderId;
    final int issueType;
    final String description;
    // Guarded by Solution.assignLock
    IssueStatus status;
    String assignedAgentId;

    public Issue(String issueId, String orderId, int issueType, String description) {
        this.issueId = issueId;
        this.orderId = orderId;
        this.issueType = issueType;
        this.description = description;
        this.status = IssueStatus.OPEN;
    }
}
