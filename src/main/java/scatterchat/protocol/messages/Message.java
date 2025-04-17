package scatterchat.protocol.messages;


public abstract class Message {

    public enum MessageType {
        CHAT_MESSAGE,
        CHAT_EXIT_MESSAGE,
        SERVER_STATE_REQUEST,
        SERVER_STATE_RESPONSE,
        SERVE_TOPIC_REQUEST,
        SERVE_TOPIC_RESPONSE,
        USERS_ORSET_MESSAGE,
    }

    private MessageType type;
    private String topic;
    private String sender;


    public Message(MessageType type) {
        this.type = type;
        this.topic = null;
        this.sender = null;
    }

    public Message(MessageType type, String topic) {
        this.type = type;
        this.topic = topic;
        this.sender = null;
    }

    public Message(MessageType type, String topic, String sender) {
        this.type = type;
        this.topic = topic;
        this.sender = sender;
    }

    public MessageType getType() {
        return this.type;
    }

    public String getTopic() {
        return this.topic;
    }

    public String getSender() {
        return this.sender;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setSender(String sender){
        this.sender = sender;
    }

    public abstract byte[] serialize();

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("type: " + type.name());
        buffer.append("\t topic: " + topic);
        buffer.append("\t sender: " + this.sender);
        return buffer.toString();
    }
}