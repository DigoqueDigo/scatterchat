package scatterchat.protocol.message.aggr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;


public class Aggr extends Message {

    private String topic;
    private List<AggrEntry> entries;

    public Aggr() {
        super(MessageType.AGGR);
    }

    public Aggr(String topic, List<AggrEntry> entries) {
        super(MessageType.AGGR);
        this.topic = topic;
        this.entries = entries;

    }

    public String getTopic() {
        return this.topic;
    }

    public List<AggrEntry> getEntries() {
        return this.entries;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(Aggr.class);
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
        kryo.register(ArrayList.class);

        Aggr aggr = kryo.readObject(input, Aggr.class);
        input.close();

        return aggr;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append("\t topic: " + this.topic);
        buffer.append("\t entries: " + this.entries);
        return buffer.toString();
    }   
}