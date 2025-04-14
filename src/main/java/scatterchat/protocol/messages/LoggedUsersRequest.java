package scatterchat.protocol.messages;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;


public class LoggedUsersRequest extends Message{


    public LoggedUsersRequest(String topic){
        super(MESSAGE_TYPE.LOGGED_USERS_REQUEST, topic);
    }


    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(LoggedUsersRequest.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public LoggedUsersRequest deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(LoggedUsersRequest.class);

        LoggedUsersRequest loggedUsersRequest = kryo.readObject(input, LoggedUsersRequest.class);
        input.close();

        return loggedUsersRequest;
    }
}