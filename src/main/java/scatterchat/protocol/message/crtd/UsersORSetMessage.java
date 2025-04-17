package scatterchat.protocol.message.crtd;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.crdt.CRDTEntry;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.crtd.ORSetMessage.Operation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;


public class UsersORSetMessage extends Message{

    private ORSetMessage orSetMessage;

    public UsersORSetMessage() {
        super(MessageType.USERS_ORSET_MESSAGE);
        this.orSetMessage = null;
    }

    public UsersORSetMessage(String topic, String sender, ORSetMessage orSetMessage){
        super(MessageType.USERS_ORSET_MESSAGE, topic, sender);
        this.orSetMessage = orSetMessage;
    }

    public ORSetMessage getOrSetMessage(){
        return this.orSetMessage;
    }

    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(UsersORSetMessage.class);
        kryo.register(ORSetMessage.class);
        kryo.register(Operation.class);
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

        kryo.register(MessageType.class);
        kryo.register(UsersORSetMessage.class);
        kryo.register(ORSetMessage.class);
        kryo.register(Operation.class);
        kryo.register(CRDTEntry.class);
        kryo.register(HashSet.class);

        UsersORSetMessage usersORSetMessage = kryo.readObject(input, UsersORSetMessage.class);
        input.close();

        return usersORSetMessage;
    }

    public String toString(){
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.toString());
        buffer.append(this.orSetMessage);
        return buffer.toString();
    }
}