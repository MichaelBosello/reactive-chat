package backend.chatroommicroservice.cluster.message;

import java.io.Serializable;

public class ChatCreatedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;

    public ChatCreatedMessage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
