package scatterchat.protocol.messages.crtd;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.crdt.CRDTEntry;
import scatterchat.protocol.messages.Message;


public class UsersORSetMessage extends Message{

    private ORSetMessage orSetMessage;


    public UsersORSetMessage(String topic, String sender, ORSetMessage orSetMessage){
        super(MESSAGE_TYPE.USERS_ORSET_MESSAGE, topic, sender);
        this.orSetMessage = orSetMessage;
    }


    public ORSetMessage getOrSetMessage(){
        return this.orSetMessage;
    }


    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(UsersORSetMessage.class);
        kryo.register(CRDTEntry.class);
        kryo.register(HashSet.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public static UsersORSetMessage deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(UsersORSetMessage.class);
        kryo.register(CRDTEntry.class);
        kryo.register(HashSet.class);

        UsersORSetMessage usersORSetMessage = kryo.readObject(input, UsersORSetMessage.class);
        input.close();

        return usersORSetMessage;
    }
}