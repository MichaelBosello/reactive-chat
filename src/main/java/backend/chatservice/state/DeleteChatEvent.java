package backend.chatservice.state;

import java.io.Serializable;

public class DeleteChatEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;

    public DeleteChatEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
