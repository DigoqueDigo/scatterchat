package scatterchat.protocol.message.info;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;


public class ServeTopicRequest extends Message {

    private String topic;
    private Set<String> nodes;

    public ServeTopicRequest() {
        super(MessageType.SERVE_TOPIC_REQUEST);
        this.nodes = new HashSet<>();
    }

    public ServeTopicRequest(String topic, Set<String> nodes) {
        super(MessageType.SERVE_TOPIC_REQUEST);
        this.topic = topic;
        this.nodes = new HashSet<>(nodes);
    }

    public String getTopic() {
        return this.topic;
    }

    public Set<String> getNodes() {
        return this.nodes;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(ServeTopicRequest.class);
        kryo.register(HashSet.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static ServeTopicRequest deserialize(byte[] data) {
        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(ServeTopicRequest.class);
        kryo.register(HashSet.class);

        ServeTopicRequest serveTopicRequest = kryo.readObject(input, ServeTopicRequest.class);
        input.close();

        return serveTopicRequest;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append("\t topic: " + this.topic);
        buffer.append("\t nodes" + this.nodes);
        return buffer.toString();
    }
}