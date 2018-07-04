package frontend.data;

public class NewMessage {

    private String chatId;
    private String userId;
    private String message;

    public NewMessage() {
    }

    public NewMessage(String chatId, String userId, String message) {
        this.chatId = chatId;
        this.userId = userId;
        this.message = message;
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
}
