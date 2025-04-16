package scatterchat.protocol.messages.info;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.messages.Message;


public class ServerStateRequest extends Message{


    public ServerStateRequest(){
        super(MESSAGE_TYPE.SERVER_STATE_REQUEST);
    }


    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ServerStateRequest.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public static ServerStateRequest deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ServerStateRequest.class);

        ServerStateRequest serverStateRequest = kryo.readObject(input, ServerStateRequest.class);
        input.close();

        return serverStateRequest;
    }
}