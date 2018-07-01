package backend.registrymicroservice.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RegistryState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<Integer, Set<Integer>> roomList;

    public RegistryState() {
        this(new HashMap<>());
    }

    public RegistryState(HashMap<Integer, Set<Integer>> roomList) {
        this.roomList = roomList;
    }

    public RegistryState copy() {
        return new RegistryState(new HashMap<>(roomList));
    }

    public void update(AddUserEvent event) {
        if (roomList.containsKey(event.getChatId())) {
            Set<Integer> room = new HashSet<>();
            room.add(event.getUserId());
            roomList.put(event.getChatId(), room);
        } else {
            roomList.get(event.getUserId()).add(event.getUserId());
        }

    }

    public void update(RemoveUserEvent event) {
        roomList.get(event.getUserId()).remove(event.getUserId());
    }

    public int size() {
        return roomList.size();
    }

    @Override
    public String toString() {
        return roomList.toString();
    }
}
