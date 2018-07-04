package backend.microservice.messagemanagermicroservice.state;

import java.io.Serializable;

public class UpdateIndexEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String chatId;

    public UpdateIndexEvent(String chatId) {
        this.chatId = chatId;
    }

    public String getChatId() {
        return chatId;
    }
}
