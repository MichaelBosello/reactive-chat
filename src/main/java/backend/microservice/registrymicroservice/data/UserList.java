package backend.microservice.registrymicroservice.data;

import java.util.Set;

public class UserList {

    private Set<String> users;

    public UserList() { }

    public UserList(Set<String> users) {
        this.users = users;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }
}
