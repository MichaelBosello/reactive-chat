package frontend.message;

import java.io.Serializable;

public class SendMessage implements Serializable {

    private final String message;

    public SendMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
