package scatterchat.protocol.messages.crtd;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.crdt.CRDTEntry;
import scatterchat.protocol.messages.Message;


public class UsersORSetMessage extends Message{

    public enum OPERATION{
        ADD,
        REMOVE,
    };

    private String element;
    private CRDTEntry clock;
    private OPERATION operation;
    private Set<CRDTEntry> entries;


    public UsersORSetMessage(String topic, String sender, OPERATION operation, String element, CRDTEntry clock, Set<CRDTEntry> entries){
        super(MESSAGE_TYPE.USERS_ORSET_MESSAGE, topic, sender);
        this.operation = operation;
        this.element = element;
        this.clock = clock;
        this.entries = entries;
    }


    public UsersORSetMessage(String topic, String sender, OPERATION operation, String element, Set<CRDTEntry> entries){
        super(MESSAGE_TYPE.USERS_ORSET_MESSAGE, topic, sender);
        this.operation = operation;
        this.element = element;
        this.clock = null;
        this.entries = entries;
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