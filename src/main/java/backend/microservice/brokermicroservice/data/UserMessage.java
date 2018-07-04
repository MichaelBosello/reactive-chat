package backend.microservice.brokermicroservice.data;

public class UserMessage {

    private String chatId;
    private String userId;
    private String message;
    private boolean chatChanged;
    private int index;

    public UserMessage() {
    }

    public UserMessage(String chatId, String userId, String message, boolean chatChanged, int index) {
        this.chatId = chatId;
        this.userId = userId;
        this.message = message;
        this.chatChanged = chatChanged;
        this.index = index;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
