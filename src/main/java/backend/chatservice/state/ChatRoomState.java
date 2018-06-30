package backend.chatservice.state;

import java.io.Serializable;
import java.util.ArrayList;

public class ChatRoomState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ArrayList<String> events;

    public ChatRoomState() {
        this(new ArrayList<>());
    }

    public ChatRoomState(ArrayList<String> events) {
        this.events = events;
    }

    public ChatRoomState copy() {
        return new ChatRoomState(new ArrayList<>(events));
    }

    public void update(NewChatEvent event) {
        events.add(event.getId());
    }

    public void update(DeleteChatEvent event) {
        events.remove(event.getId());
    }

    public int size() {
        return events.size();
    }

    @Override
    public String toString() {
        return events.toString();
    }
}
