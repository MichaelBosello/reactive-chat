package backend.microservice.registrymicroservice.message;

import java.io.Serializable;
import java.util.Set;

public class UserListMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String chatId;
    private final Set<String> users;

    public UserListMessage(String chatId, Set<String> users) {
        this.chatId = chatId;
        this.users = users;
    }

    public String getChatId() {
        return chatId;
    }

    public Set<String> getUsers() {
        return users;
    }
}
