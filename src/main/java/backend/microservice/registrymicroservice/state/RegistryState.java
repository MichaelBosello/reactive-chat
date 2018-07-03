package backend.microservice.registrymicroservice.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RegistryState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Set<String>> roomList;

    public RegistryState() {
        this(new HashMap<>());
    }

    public RegistryState(HashMap<String, Set<String>> roomList) {
        this.roomList = roomList;
    }

    public RegistryState copy() {
        return new RegistryState(new HashMap<>(roomList));
    }

    public void update(AddUserEvent event) {
        if (roomList.containsKey(event.getChatId())) {
            Set<String> room = new HashSet<>();
            room.add(event.getUserId());
            roomList.put(event.getChatId(), room);
        } else {
            roomList.get(event.getUserId()).add(event.getUserId());
        }

    }

    public void update(RemoveUserEvent event) {
        if(roomList.get(event.getUserId()).contains(event.getUserId()))
            roomList.get(event.getUserId()).remove(event.getUserId());
    }

    public Set<String> getUsers(String chatId){
        return roomList.get(chatId);
    }

    public int size() {
        return roomList.size();
    }

    @Override
    public String toString() {
        return roomList.toString();
    }
}
