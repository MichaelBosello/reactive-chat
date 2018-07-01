package backend.chatroommicroservice.message;

import java.io.Serializable;

public class ChatAlreadyExistMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;

    public ChatAlreadyExistMessage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
