package frontend.chatgui;

public interface ChatGUI {

    void newMessage(String message);

    void connected();

    void connectionError(String error);

    void newChatCreated();

    void newChatError(String error);

    void addObserver(ChatObserver observer);

}
