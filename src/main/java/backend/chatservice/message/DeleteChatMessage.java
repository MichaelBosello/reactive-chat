package backend.chatservice.message;

import java.io.Serializable;

public class DeleteChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;

    public DeleteChatMessage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
