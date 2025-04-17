package scatterchat.protocol.message.chat;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class TopicEnterMessage extends Message {

    public TopicEnterMessage() {
        super(MessageType.TOPIC_ENTER_MESSAGE);
    }

    public TopicEnterMessage(String topic, String sender) {
        super(MessageType.TOPIC_ENTER_MESSAGE, topic, sender);
    }

    public byte[] serialize() {

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(TopicEnterMessage.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static TopicEnterMessage deserialize(byte[] data) {

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(TopicEnterMessage.class);

        TopicEnterMessage topicEnterMessage = kryo.readObject(input, TopicEnterMessage.class);
        input.close();

        return topicEnterMessage;
    }
}