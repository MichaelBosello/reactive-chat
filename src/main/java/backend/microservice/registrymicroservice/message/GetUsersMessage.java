package backend.microservice.registrymicroservice.message;

import java.io.Serializable;

public class GetUsersMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String chatId;

    public GetUsersMessage(String chatId) {
        this.chatId = chatId;
    }

    public String getChatId() {
        return chatId;
    }
}
