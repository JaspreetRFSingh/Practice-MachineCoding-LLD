package JobScheduler;

import java.util.*;

public class Solution {

    private final Map<String, Machine> machines = new LinkedHashMap<>();
    // jobId -> machineId
    private final Map<String, String> jobToMachine = new HashMap<>();

    private final Map<Integer, AssignmentCriteria> criteriaRegistry = new HashMap<>();

    public Solution() {
        criteriaRegistry.put(0, new LeastUnfinishedCriteria());
        criteriaRegistry.put(1, new MostFinishedCriteria());
    }

    public void addMachine(String machineId, String[] capabilities) {
        machines.put(machineId, new Machine(machineId, capabilities));
    }

    public String assignMachineToJob(String jobId, String[] capabilitiesRequired, int criteria) {
        AssignmentCriteria strategy = criteriaRegistry.get(criteria);
        if (strategy == null) return "";

        // Filter compatible machines
        List<Machine> candidates = new ArrayList<>();
        for (Machine m : machines.values()) {
            if (m.hasCapabilities(capabilitiesRequired)) {
                candidates.add(m);
            }
        }
        if (candidates.isEmpty()) return "";

        // Sort by criteria, then by lexicographically smallest machineId as tiebreaker
        Comparator<Machine> comparator = strategy.getComparator()
                .thenComparing(Machine::getMachineId);
        candidates.sort(comparator);

        Machine selected = candidates.get(0);
        selected.assignJob();
        jobToMachine.put(jobId, selected.getMachineId());
        return selected.getMachineId();
    }

    public void jobCompleted(String jobId) {
        String machineId = jobToMachine.get(jobId);
        if (machineId != null) {
            machines.get(machineId).completeJob();
        }
    }
}
