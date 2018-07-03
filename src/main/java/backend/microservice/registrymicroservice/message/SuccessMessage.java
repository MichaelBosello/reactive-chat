package backend.microservice.registrymicroservice.message;

import java.io.Serializable;

public class SuccessMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String chatId;
    private final String userId;

    public SuccessMessage(String chatId, String userId) {
        this.chatId = chatId;
        this.userId = userId;
    }

    public String getChatId() {
        return chatId;
    }

    public String getUserId() {
        return userId;
    }
}
