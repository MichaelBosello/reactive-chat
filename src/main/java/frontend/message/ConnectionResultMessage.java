package frontend.message;

import java.io.Serializable;

public class ConnectionResultMessage implements Serializable {

    private final boolean success;
    private final String error;

    public ConnectionResultMessage(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
