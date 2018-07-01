package backend.chatroommicroservice.state;

import java.io.Serializable;

public class NewChatEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;

    public NewChatEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
