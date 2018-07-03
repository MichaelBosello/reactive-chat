package backend.microservice.registrymicroservice.data;

import java.util.Set;

public class UserList {

    private String chatId;

    private Set<String> users;

    public UserList() { }

    public UserList(Set<String> users, String chatId) {
        this.users = users;
        this.chatId = chatId;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
}
