package backend.microservice.messagemanagermicroservice.data;

public class UserMessage {

    private String chatId;
    private String userId;
    private String message;
    private boolean chatChanged;

    public UserMessage() {
    }

    public UserMessage(String chatId, String userId, String message, boolean chatChanged) {
        this.chatId = chatId;
        this.userId = userId;
        this.message = message;
        this.chatChanged = chatChanged;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isChatChanged() {
        return chatChanged;
    }

    public void setChatChanged(boolean chatChanged) {
        this.chatChanged = chatChanged;
    }
}
