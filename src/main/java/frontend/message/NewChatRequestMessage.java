package frontend.message;

import java.io.Serializable;

public class NewChatRequestMessage implements Serializable {

    private final String host;

    public NewChatRequestMessage(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }
}
