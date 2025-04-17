package scatterchat.protocol.message.chat;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;


public class ChatMessage extends Message {

    private final String message;

    public ChatMessage() {
        super(MessageType.CHAT_MESSAGE);
        this.message = null;
    }

    public ChatMessage(String topic, String message) {
        super(MessageType.CHAT_MESSAGE, topic);
        this.message = message;
    }

    public ChatMessage(String topic, String sender, String message) {
        super(MessageType.CHAT_MESSAGE, topic, sender);
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(ChatMessage.class);
        kryo.register(HashMap.class);
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
        kryo.register(ChatMessage.class);
        kryo.register(HashMap.class);

        ChatMessage chatMessage = kryo.readObject(input, ChatMessage.class);
        input.close();

        return chatMessage;
    }

    public String toString() {
        return super.toString() + "\t message: " + message;
    }
}