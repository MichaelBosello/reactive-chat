package frontend.message;

import java.io.Serializable;
import java.util.List;

public class NextMessage implements Serializable {
    private final String message;
    private final int index;

    public NextMessage(String message, int index) {
        this.message = message;
        this.index = index;
    }

    public String getMessage() {
        return message;
    }

    public int getIndex() {
        return index;
    }
}
