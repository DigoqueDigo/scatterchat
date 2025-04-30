package scatterchat.protocol.message;


public abstract class Message {

    public enum MessageType {
        CHAT_MESSAGE,
        SERVER_STATE_REQUEST,
        SERVER_STATE_RESPONSE,
        SERVE_TOPIC_REQUEST,
        SERVE_TOPIC_RESPONSE,
        TOPIC_ENTER_MESSAGE,
        TOPIC_EXIT_MESSAGE,
        USERS_ORSET_MESSAGE,
        HEART_BEAT,
        CYCLON,
        CYCLON_OK,
        CYCLON_ERROR,
        AGGR_REQ,
        AGGR_REP,
        AGGR,
    }

    private MessageType type;
    private String sender;
    private String receiver;


    public Message(MessageType type) {
        this.type = type;
        this.sender = null;
        this.receiver = null;
    }

    public Message(MessageType type, String sender, String receiver) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
    }

    public MessageType getType() {
        return this.type;
    }

    public String getSender() {
        return this.sender;
    }

    public String getReceiver() {
        return this.receiver;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public abstract byte[] serialize();

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(type.name());
        buffer.append(", ").append(this.sender);
        buffer.append(", ").append(this.receiver);
        return buffer.toString();
    }
}