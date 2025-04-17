package scatterchat.protocol.message;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.clock.VectorClock;
import scatterchat.crdt.CRDTEntry;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.chat.ChatExitMessage;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.ORSetMessage;
import scatterchat.protocol.message.crtd.ORSetMessage.Operation;
import scatterchat.protocol.message.crtd.UsersORSetMessage;
import scatterchat.protocol.message.info.ServeTopicRequest;
import scatterchat.protocol.message.info.ServeTopicResponse;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class CausalMessage {

    private Message message;
    private VectorClock vectorClock;

    public CausalMessage() {
        this.message = null;
        this.vectorClock = null;
    }

    public CausalMessage(Message message, VectorClock vectorClock) {
        this.message = message;
        this.vectorClock = vectorClock;
    }

    public Message getMessage() {
        return this.message;
    }

    public VectorClock getVectorClock() {
        return this.vectorClock;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(VectorClock.class);
        kryo.register(CausalMessage.class);

        kryo.register(Message.class);
        kryo.register(ChatMessage.class);
        kryo.register(ChatExitMessage.class);
        kryo.register(UsersORSetMessage.class);
        kryo.register(ServerStateRequest.class);
        kryo.register(ServerStateResponse.class);
        kryo.register(ServeTopicRequest.class);
        kryo.register(ServeTopicResponse.class);

        kryo.register(ORSetMessage.class);
        kryo.register(Operation.class);
        kryo.register(CRDTEntry.class);
        kryo.register(MessageType.class);

        kryo.register(HashMap.class);
        kryo.register(HashSet.class);
        kryo.register(ArrayList.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static CausalMessage deserialize(byte[] data) {
        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(VectorClock.class);
        kryo.register(CausalMessage.class);

        kryo.register(Message.class);
        kryo.register(ChatMessage.class);
        kryo.register(ChatExitMessage.class);
        kryo.register(UsersORSetMessage.class);
        kryo.register(ServerStateRequest.class);
        kryo.register(ServerStateResponse.class);
        kryo.register(ServeTopicRequest.class);
        kryo.register(ServeTopicResponse.class);

        kryo.register(ORSetMessage.class);
        kryo.register(Operation.class);
        kryo.register(CRDTEntry.class);
        kryo.register(MessageType.class);

        kryo.register(HashMap.class);
        kryo.register(HashSet.class);
        kryo.register(ArrayList.class);

        CausalMessage causalMessage = kryo.readObject(input, CausalMessage.class);
        input.close();

        return causalMessage;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.message);
        buffer.append("\t clock: " + this.vectorClock);
        return buffer.toString();
    }
}