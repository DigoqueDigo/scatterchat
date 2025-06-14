package scatterchat.protocol.message.aggr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatServerEntry;


public class Aggr extends Message {

    private String topic;
    private List<AggrEntry> entries;

    public Aggr() {
        super(MessageType.AGGR);
    }

    public Aggr(String sender, String receiver, String topic, List<AggrEntry> entries) {
        super(MessageType.AGGR, sender, receiver);
        this.topic = topic;
        this.entries = new ArrayList<>(entries);
    }

    public String getTopic() {
        return this.topic;
    }

    public List<AggrEntry> getEntries() {
        return new ArrayList<>(this.entries);
    }

    public byte[] serialize() {

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(Aggr.class);
        kryo.register(AggrEntry.class);
        kryo.register(ChatServerEntry.class);
        kryo.register(ArrayList.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static Aggr deserialize(byte[] data) {

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(Aggr.class);
        kryo.register(AggrEntry.class);
        kryo.register(ChatServerEntry.class);
        kryo.register(ArrayList.class);

        Aggr aggr = kryo.readObject(input, Aggr.class);
        input.close();

        return aggr;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append(", ").append(this.topic);
        buffer.append(", ").append(this.entries);
        return buffer.toString();
    }
}