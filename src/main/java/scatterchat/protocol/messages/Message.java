package scatterchat.protocol.messages;


public abstract class Message{

    public enum MESSAGE_TYPE{
        CHAT_MESSAGE,
        LOGGED_USERS_REQUEST,
        GROUP_JOIN_WARNING,
    }

    private MESSAGE_TYPE type;
    private String topic;


    public Message(MESSAGE_TYPE type, String topic){
        this.type = type;
        this.topic = topic;
    }


    public MESSAGE_TYPE getType(){
        return this.type;
    }


    public String getTopic(){
        return this.topic;
    }


    public void setTopic(String topic){
        this.topic = topic;
    }


    public abstract byte[] serialize();


    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append("type: " + type.name());
        buffer.append("\t topic: " + topic);
        return buffer.toString();
    }
}