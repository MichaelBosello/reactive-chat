package frontend.chatgui;

public interface ChatObserver {

    void joinEvent(String name);

    void leaveEvent(String name);

    void newChatEvent(String name);

    void sendEvent(String message);
}
