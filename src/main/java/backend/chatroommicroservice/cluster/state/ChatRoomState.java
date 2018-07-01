package backend.chatroommicroservice.cluster.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChatRoomState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<String> events;

    public ChatRoomState() {
        this(new ArrayList<>());
    }

    public ChatRoomState(List<String> events) {
        this.events = events;
    }

    public ChatRoomState copy() {
        return new ChatRoomState(new ArrayList<>(events));
    }

    public void update(NewChatEvent event) {
        events.add(event.getId());
    }

    public int size() {
        return events.size();
    }

    public List<String> getRoom(){
        return events;
    }

    @Override
    public String toString() {
        return events.toString();
    }
}
