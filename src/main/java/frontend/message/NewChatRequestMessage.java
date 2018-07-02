package frontend.message;

import java.io.Serializable;

public class NewChatRequestMessage implements Serializable {

    private final String name;

    public NewChatRequestMessage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
