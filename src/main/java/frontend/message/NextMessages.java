package frontend.message;

import java.io.Serializable;
import java.util.List;

public class NextMessages implements Serializable {
    private final List<String> message;

    public NextMessages(List<String> message) {
        this.message = message;
    }

    public List<String> getMessage() {
        return message;
    }
}
