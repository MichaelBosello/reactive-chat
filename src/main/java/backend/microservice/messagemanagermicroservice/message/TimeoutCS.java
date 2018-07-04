package backend.microservice.messagemanagermicroservice.message;

import java.io.Serializable;

public class TimeoutCS implements Serializable {

    private final String chatId;

    public TimeoutCS(String chatId) {
        this.chatId = chatId;
    }

    public String getChatId() {
        return chatId;
    }
}
