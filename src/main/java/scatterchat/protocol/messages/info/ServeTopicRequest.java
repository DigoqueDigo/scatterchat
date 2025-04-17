package scatterchat.protocol.messages.info;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.messages.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


public class ServeTopicRequest extends Message {

    private List<String> nodes;

    public ServeTopicRequest(String topic, List<String> nodes) {
        super(MessageType.SERVE_TOPIC_REQUEST, topic);
        this.nodes = new ArrayList<>(nodes);
    }

    public List<String> getNodes() {
        return this.nodes;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(ServeTopicRequest.class);
        kryo.register(ArrayList.class);
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
        kryo.reference(ArrayList.class);

        ServeTopicRequest serveTopicRequest = kryo.readObject(input, ServeTopicRequest.class);
        input.close();

        return serveTopicRequest;
    }

    public String toString() {
        return super.toString() + "\t nodes: " + nodes;
    }
}