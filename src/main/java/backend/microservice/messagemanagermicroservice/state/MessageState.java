package backend.microservice.messagemanagermicroservice.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MessageState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Integer> messageIndex;

    public MessageState() {
        this(new HashMap<>());
    }

    public MessageState(Map<String, Integer> messageIndex) {
        this.messageIndex = messageIndex;
    }

    public MessageState copy() {
        return new MessageState(new HashMap<>(messageIndex));
    }

    public void update(UpdateIndexEvent event) {
        int next = messageIndex.get(event.getChatId()) + 1;
        messageIndex.replace(event.getChatId(), next);
    }

    public int size() {
        return messageIndex.size();
    }

    public int getIndex(String chat){
        return messageIndex.getOrDefault(chat, 0);
    }

    @Override
    public String toString() {
        return messageIndex.toString();
    }
}
