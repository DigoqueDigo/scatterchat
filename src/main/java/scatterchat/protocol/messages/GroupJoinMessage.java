package scatterchat.protocol.messages;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


public class GroupJoinMessage extends Message{

    private List<String> nodes;


    public GroupJoinMessage(String topic, List<String> nodes){
        super(MESSAGE_TYPE.GROUP_JOIN_WARNING, topic);
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
        kryo.register(GroupJoinMessage.class);
        kryo.register(ArrayList.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public static GroupJoinMessage deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(GroupJoinMessage.class);
        kryo.reference(ArrayList.class);

        GroupJoinMessage groupJoinMessage = kryo.readObject(input, GroupJoinMessage.class);
        input.close();

        return groupJoinMessage;
    }


    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append("\t nodes: " + nodes);
        return buffer.toString();
    }
}