package backend.chatroommicroservice.message;

import java.io.Serializable;

public class NewChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;

    public NewChatMessage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
