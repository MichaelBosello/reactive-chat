package backend.microservice.messagemanagermicroservice.message;

import java.io.Serializable;

public class NewMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String chatId;
    private final String userId;
    private final String message;
    private final boolean chatChanged;

    public NewMessage(String chatId, String userId, String message, boolean chatChanged) {
        this.chatId = chatId;
        this.userId = userId;
        this.message = message;
        this.chatChanged = chatChanged;
    }

    public String getChatId() {
        return chatId;
    }

    public String getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isChatChanged() {
        return chatChanged;
    }
}
