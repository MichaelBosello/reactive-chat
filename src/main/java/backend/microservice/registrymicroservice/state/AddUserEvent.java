package backend.microservice.registrymicroservice.state;

import java.io.Serializable;

public class AddUserEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int chatId;
    private final int userId;

    public AddUserEvent(int chatId, int userId) {
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
