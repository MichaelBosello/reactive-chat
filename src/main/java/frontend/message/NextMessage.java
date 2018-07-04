package frontend.message;

import java.io.Serializable;
import java.util.List;

public class NextMessage implements Serializable {
    private final String message;

    public NextMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
