package frontend.message;

import java.io.Serializable;

public class LeaveRequestMessage implements Serializable {

    private final String name;

    public LeaveRequestMessage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
