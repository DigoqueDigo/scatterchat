package scatterchat.protocol.message.chat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;


public class HeartBeatMessage extends Message {

    private String topic;

    public HeartBeatMessage() {
        super(MessageType.HEART_BEAT);
        this.topic = null;
    }

    public HeartBeatMessage(String sender, String receiver, String topic) {
        super(MessageType.HEART_BEAT, sender, receiver);
        this.topic = topic;
    }

    public String getTopic() {
        return this.topic;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(HeartBeatMessage.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static ChatMessage deserialize(byte[] data) {
        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(HeartBeatMessage.class);

        ChatMessage chatMessage = kryo.readObject(input, ChatMessage.class);
        input.close();

        return chatMessage;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append(", ").append(this.topic);
        return buffer.toString();
    }
}