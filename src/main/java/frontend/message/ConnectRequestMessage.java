package frontend.message;

import java.io.Serializable;

public class ConnectRequestMessage implements Serializable {

    private final String name;

    public ConnectRequestMessage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
