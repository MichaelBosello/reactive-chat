package backend.microservice.brokermicroservice.data;

public class NextMessageData {

    private String chatId;
    private String userId;
    private String message;
    private int index;

    public NextMessageData() {
    }

    public NextMessageData(String chatId, String userId, String message, int index) {
        this.chatId = chatId;
        this.userId = userId;
        this.message = message;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
