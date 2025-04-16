package scatterchat.protocol.messages.info;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.messages.Message;


public class ServeTopicRequest extends Message{

    private List<String> nodes;


    public ServeTopicRequest(String topic, List<String> nodes){
        super(MESSAGE_TYPE.SERVE_TOPIC_REQUEST, topic);
        this.nodes = new ArrayList<>(nodes);
    }


    public List<String> getNodes(){
        return this.nodes;
    }


    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ServeTopicRequest.class);
        kryo.register(ArrayList.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public static ServeTopicRequest deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ServeTopicRequest.class);
        kryo.reference(ArrayList.class);

        ServeTopicRequest serveTopicRequest = kryo.readObject(input, ServeTopicRequest.class);
        input.close();

        return serveTopicRequest;
    }


    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append("\t nodes: " + nodes);
        return buffer.toString();
    }
}