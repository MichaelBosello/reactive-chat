package backend.microservice.registrymicroservice.state;

import java.io.Serializable;

public class RemoveUserEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int chatId;
    private final int userId;

    public RemoveUserEvent(int chatId, int userId) {
        this.chatId = chatId;
        this.userId = userId;
    }

    public int getChatId() {
        return chatId;
    }

    public int getUserId() {
        return userId;
    }
}
