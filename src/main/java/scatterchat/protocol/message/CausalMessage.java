package scatterchat.protocol.message;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.clock.VectorClock;
import scatterchat.crdt.ORSetEntry;
import scatterchat.crdt.ORSetAction;
import scatterchat.crdt.ORSetAction.Operation;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;
import scatterchat.protocol.message.info.ServeTopicRequest;
import scatterchat.protocol.message.info.ServeTopicResponse;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;


public class CausalMessage {

    private String topic;
    private Message message;
    private VectorClock vectorClock;

    public CausalMessage() {
        this.message = null;
        this.vectorClock = null;
        this.topic = null;
    }

    public CausalMessage(String topic, Message message, VectorClock vectorClock) {
        this.topic = topic;
        this.message = message;
        this.vectorClock = vectorClock;
    }

    public String getTopic() {
        return this.topic;
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
        kryo.register(TopicEnterMessage.class);
        kryo.register(TopicExitMessage.class);
        kryo.register(UserORSetMessage.class);
        kryo.register(ServerStateRequest.class);
        kryo.register(ServerStateResponse.class);
        kryo.register(ServeTopicRequest.class);
        kryo.register(ServeTopicResponse.class);

        kryo.register(ORSetAction.class);
        kryo.register(ORSetEntry.class);
        kryo.register(Operation.class);
        kryo.register(ChatServerEntry.class);
        kryo.register(MessageType.class);

        kryo.register(HashMap.class);
        kryo.register(HashSet.class);
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
        kryo.register(TopicEnterMessage.class);
        kryo.register(TopicExitMessage.class);
        kryo.register(UserORSetMessage.class);
        kryo.register(ServerStateRequest.class);
        kryo.register(ServerStateResponse.class);
        kryo.register(ServeTopicRequest.class);
        kryo.register(ServeTopicResponse.class);

        kryo.register(ORSetAction.class);
        kryo.register(ORSetEntry.class);
        kryo.register(Operation.class);
        kryo.register(ChatServerEntry.class);
        kryo.register(MessageType.class);

        kryo.register(HashMap.class);
        kryo.register(HashSet.class);

        CausalMessage causalMessage = kryo.readObject(input, CausalMessage.class);
        input.close();

        return causalMessage;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.message);
        buffer.append(", ").append(topic);
        buffer.append(", ").append(this.vectorClock);
        return buffer.toString();
    }
}