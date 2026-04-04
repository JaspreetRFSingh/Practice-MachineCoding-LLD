package JobScheduler;

import java.util.HashSet;
import java.util.Set;

public class Machine {
    private final String machineId;
    private final Set<String> capabilities;
    private int unfinishedCount;
    private int finishedCount;

    public Machine(String machineId, String[] capabilities) {
        this.machineId = machineId;
        this.capabilities = new HashSet<>();
        for (String cap : capabilities) {
            this.capabilities.add(cap.toLowerCase());
        }
        this.unfinishedCount = 0;
        this.finishedCount = 0;
    }

    public String getMachineId() { return machineId; }
    public int getUnfinishedCount() { return unfinishedCount; }
    public int getFinishedCount() { return finishedCount; }

    public boolean hasCapabilities(String[] required) {
        for (String cap : required) {
            if (!capabilities.contains(cap.toLowerCase())) return false;
        }
        return true;
    }

    public void assignJob() { unfinishedCount++; }

    public void completeJob() {
        unfinishedCount--;
        finishedCount++;
    }
}
