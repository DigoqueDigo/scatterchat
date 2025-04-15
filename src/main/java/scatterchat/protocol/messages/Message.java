package scatterchat.protocol.messages;


public abstract class Message{

    public enum MESSAGE_TYPE{
        CHAT_MESSAGE,
        LOGGED_USERS_REQUEST,
        GROUP_JOIN_WARNING,
    }

    private MESSAGE_TYPE type;
    private String topic;
    private String sender;


    public Message(MESSAGE_TYPE type, String topic){
        this.type = type;
        this.topic = topic;
    }

    public Message(MESSAGE_TYPE type, String topic, String sender){
        this.type = type;
        this.topic = topic;
        this.sender = sender;
    }


    public MESSAGE_TYPE getType(){
        return this.type;
    }


    public String getTopic(){
        return this.topic;
    }


    public String getSender(){
        return this.sender;
    }


    public void setTopic(String topic){
        this.topic = topic;
    }


    public abstract byte[] serialize();


    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append("type: " + type.name());
        buffer.append("\t topic: " + topic);
        buffer.append("\t sender: " + this.sender);
        return buffer.toString();
    }
}