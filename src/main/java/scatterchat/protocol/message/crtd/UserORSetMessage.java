package scatterchat.protocol.message.crtd;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.crdt.ORSetAction;
import scatterchat.crdt.ORSetAction.Operation;
import scatterchat.crdt.ORSetEntry;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatServerEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;


public class UserORSetMessage extends Message{

    private String topic;
    private ORSetAction orSetAction;

    public UserORSetMessage() {
        super(MessageType.USERS_ORSET_MESSAGE);
        this.orSetAction = null;
    }

    public UserORSetMessage(String sender, String receiver, String topic, ORSetAction orSetAction) {
        super(MessageType.USERS_ORSET_MESSAGE, sender, receiver);
        this.topic = topic;
        this.orSetAction = orSetAction;
    }

    public String getTopic() {
        return this.topic;
    }

    public ORSetAction getORSetAction(){
        return this.orSetAction;
    }

    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(UserORSetMessage.class);
        kryo.register(ORSetAction.class);
        kryo.register(Operation.class);
        kryo.register(ORSetEntry.class);
        kryo.register(ChatServerEntry.class);
        kryo.register(HashSet.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static UserORSetMessage deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(UserORSetMessage.class);
        kryo.register(ORSetAction.class);
        kryo.register(Operation.class);
        kryo.register(ORSetEntry.class);
        kryo.register(ChatServerEntry.class);
        kryo.register(HashSet.class);

        UserORSetMessage userORSetAction = kryo.readObject(input, UserORSetMessage.class);
        input.close();

        return userORSetAction;
    }

    public String toString(){
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.toString());
        buffer.append(", ").append(this.topic);
        buffer.append(", ").append(this.orSetAction);
        return buffer.toString();
    }
}