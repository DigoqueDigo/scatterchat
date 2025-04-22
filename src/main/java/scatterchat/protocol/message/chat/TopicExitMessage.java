package scatterchat.protocol.message.chat;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class TopicExitMessage extends Message {

    private String topic;
    private ChatServerEntry chatServerEntry;

    public TopicExitMessage() {
        super(MessageType.TOPIC_EXIT_MESSAGE);
    }

    public TopicExitMessage(String sender, String receiver, String topic, ChatServerEntry chatServerEntry) {
        super(MessageType.TOPIC_EXIT_MESSAGE, sender, receiver);
        this.topic = topic;
        this.chatServerEntry = chatServerEntry;
    }

    public String getTopic() {
        return this.topic;
    }

    public ChatServerEntry getChatServerEntry() {
        return this.chatServerEntry;
    }

    public byte[] serialize() {

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(TopicExitMessage.class);
        kryo.register(ChatServerEntry.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static TopicExitMessage deserialize(byte[] data) {

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(TopicExitMessage.class);
        kryo.register(ChatServerEntry.class);

        TopicExitMessage topicExitMessage = kryo.readObject(input, TopicExitMessage.class);
        input.close();

        return topicExitMessage;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append(", ").append(this.topic);
        buffer.append(", ").append(this.chatServerEntry);
        return buffer.toString();
    }
}