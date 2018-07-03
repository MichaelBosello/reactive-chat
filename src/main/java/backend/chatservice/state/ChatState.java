package backend.chatservice.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChatState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<String> events;

    public ChatState() {
        this(new ArrayList<>());
    }

    public ChatState(List<String> events) {
        this.events = events;
    }

    public ChatState copy() {
        return new ChatState(new ArrayList<>(events));
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
