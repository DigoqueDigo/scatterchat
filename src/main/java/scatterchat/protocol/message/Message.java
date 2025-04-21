package scatterchat.protocol.message;


public abstract class Message {

    public enum MessageType {
        CHAT_MESSAGE,
        TOPIC_ENTER_MESSAGE,
        TOPIC_EXIT_MESSAGE,
        SERVER_STATE_REQUEST,
        SERVER_STATE_RESPONSE,
        SERVE_TOPIC_REQUEST,
        SERVE_TOPIC_RESPONSE,
        USERS_ORSET_MESSAGE,
        CYCLON,
        CYCLON_OK,
        CYCLON_ERROR,
        AGGR_REQ,
        AGGR_REP,
        AGGR,
    }

    private MessageType type;
    private String sender;


    public Message(MessageType type) {
        this.type = type;
        this.sender = null;
    }

    public Message(MessageType type, String sender) {
        this.type = type;
        this.sender = null;
    }

    public MessageType getType() {
        return this.type;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender){
        this.sender = sender;
    }

    public abstract byte[] serialize();

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("type: " + type.name());
        buffer.append("\t sender: " + this.sender);
        return buffer.toString();
    }
}