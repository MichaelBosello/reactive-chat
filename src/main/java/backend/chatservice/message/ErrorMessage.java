package backend.chatservice.message;

import java.io.Serializable;

public class ErrorMessage implements Serializable {

    private final String error;

    public ErrorMessage(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
