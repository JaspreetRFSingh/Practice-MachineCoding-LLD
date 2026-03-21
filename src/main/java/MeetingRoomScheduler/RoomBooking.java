package MeetingRoomScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantLock;

public class RoomBooking {

    // All room IDs sorted lexicographically; immutable after construction
    private final List<String> roomIds;
    // roomId → index into roomSchedules / roomLocks arrays; O(1) lookup
    private final Map<String, Integer> roomIndexMap;
    // Per-room interval schedule: startTime → endTime; O(log M) floor/ceiling queries
    private final ConcurrentSkipListMap<Integer, Integer>[] roomSchedules;
    // Per-room lock for atomic check-and-book
    private final ReentrantLock[] roomLocks;

    // Rooms with zero active meetings — always available for any interval
    private final ConcurrentSkipListSet<String> freeRooms;
    // Rooms with ≥1 active meeting — complement of freeRooms; must check schedule
    private final ConcurrentSkipListSet<String> occupiedRooms;

    // meetingId → (roomId, startTime, endTime); O(1) cancel lookup
    private final ConcurrentHashMap<String, MeetingInfo> activeMeetings;

    private record MeetingInfo(String roomId, int startTime, int endTime) {}

    public RoomBooking(List<String> roomIds) {
        List<String> sortedRooms = roomIds.stream().sorted().toList();
        this.roomIds = sortedRooms;

        int n = sortedRooms.size();
        roomIndexMap = new HashMap<>();
        for (int i = 0; i < n; i++) roomIndexMap.put(sortedRooms.get(i), i);

        roomSchedules = new ConcurrentSkipListMap[n];
        roomLocks     = new ReentrantLock[n];
        for (int i = 0; i < n; i++) {
            roomSchedules[i] = new ConcurrentSkipListMap<>();
            roomLocks[i]     = new ReentrantLock();
        }

        freeRooms     = new ConcurrentSkipListSet<>(sortedRooms);
        occupiedRooms = new ConcurrentSkipListSet<>();
        activeMeetings = new ConcurrentHashMap<>();
    }

    // See notes.md — Three-Phase Booking Algorithm
    public String bookMeeting(String meetingId, int startTime, int endTime) {
        // Phase 1: check occupied rooms that are lex-smaller than the first idle room.
        // An idle room is always available, so no room after it can ever be the answer.
        String idleCandidate = freeRooms.isEmpty() ? null : freeRooms.first();

        NavigableSet<String> occupiedBefore = (idleCandidate != null)
                ? occupiedRooms.headSet(idleCandidate, false)
                : occupiedRooms;

        for (String roomId : occupiedBefore) {
            int idx = roomIndexMap.get(roomId);
            roomLocks[idx].lock();
            try {
                if (isRoomFree(idx, startTime, endTime)) {
                    roomSchedules[idx].put(startTime, endTime);
                    MeetingInfo info = new MeetingInfo(roomId, startTime, endTime);
                    MeetingInfo old  = activeMeetings.putIfAbsent(meetingId, info);
                    if (old == null) return roomId;
                    roomSchedules[idx].remove(startTime); // duplicate meetingId: rollback
                    return "";
                }
            } finally {
                roomLocks[idx].unlock();
            }
        }

        // Phase 2: no occupied room beat idleCandidate — try to book it directly
        if (idleCandidate != null) {
            int idx = roomIndexMap.get(idleCandidate);
            roomLocks[idx].lock();
            try {
                if (roomSchedules[idx].isEmpty()) {
                    roomSchedules[idx].put(startTime, endTime);
                    MeetingInfo info = new MeetingInfo(idleCandidate, startTime, endTime);
                    MeetingInfo old  = activeMeetings.putIfAbsent(meetingId, info);
                    if (old == null) {
                        freeRooms.remove(idleCandidate);
                        occupiedRooms.add(idleCandidate);
                        return idleCandidate;
                    }
                    roomSchedules[idx].remove(startTime); // duplicate meetingId: rollback
                    return "";
                }
                // Stale snapshot: room was booked between our read and lock acquisition
                freeRooms.remove(idleCandidate);
                occupiedRooms.add(idleCandidate);
            } finally {
                roomLocks[idx].unlock();
            }
        }

        // Phase 3: full linear fallback — hit only when idleCandidate was stolen (rare race)
        // or when every single room has at least one meeting
        for (String roomId : roomIds) {
            int idx = roomIndexMap.get(roomId);
            roomLocks[idx].lock();
            try {
                if (isRoomFree(idx, startTime, endTime)) {
                    roomSchedules[idx].put(startTime, endTime);
                    MeetingInfo info = new MeetingInfo(roomId, startTime, endTime);
                    MeetingInfo old  = activeMeetings.putIfAbsent(meetingId, info);
                    if (old == null) {
                        freeRooms.remove(roomId);
                        occupiedRooms.add(roomId);
                        return roomId;
                    }
                    roomSchedules[idx].remove(startTime);
                    return "";
                }
            } finally {
                roomLocks[idx].unlock();
            }
        }

        return "";
    }

    // O(log M): floor entry checks for a meeting that started earlier and overlaps;
    // ceiling entry checks for a meeting that starts within our window
    private boolean isRoomFree(int roomIndex, int start, int end) {
        ConcurrentSkipListMap<Integer, Integer> schedule = roomSchedules[roomIndex];
        Map.Entry<Integer, Integer> floor = schedule.floorEntry(start);
        if (floor != null && floor.getValue() >= start) return false;
        Map.Entry<Integer, Integer> ceil  = schedule.ceilingEntry(start);
        if (ceil  != null && ceil.getKey()   <= end)   return false;
        return true;
    }

    public boolean cancelMeeting(String meetingId) {
        MeetingInfo info = activeMeetings.get(meetingId);
        if (info == null) return false;

        int roomIndex = roomIndexMap.get(info.roomId());
        roomLocks[roomIndex].lock();
        try {
            // remove(k, v) is atomic — concurrent cancels of the same meetingId are safe
            if (!activeMeetings.remove(meetingId, info)) return false;
            roomSchedules[roomIndex].remove(info.startTime());
            if (roomSchedules[roomIndex].isEmpty()) {
                occupiedRooms.remove(info.roomId());
                freeRooms.add(info.roomId());
            }
            return true;
        } finally {
            roomLocks[roomIndex].unlock();
        }
    }
}
