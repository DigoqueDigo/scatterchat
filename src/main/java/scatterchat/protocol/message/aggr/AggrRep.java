package scatterchat.protocol.message.aggr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;


public class AggrRep extends Message {

    private String topic;
    private boolean success;

    public AggrRep() {
        super(MessageType.AGGR_REP);
    }

    public AggrRep(String topic, boolean success) {
        super(MessageType.AGGR_REP);
        this.topic = topic;
        this.success = success;
    }

    public String getTopic() {
        return this.topic;
    }

    public boolean getSuccess() {
        return this.success;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(AggrRep.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static AggrRep deserialize(byte[] data) {
        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(AggrRep.class);

        AggrRep aggrRep = kryo.readObject(input, AggrRep.class);
        input.close();

        return aggrRep;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append("\t topic: " + this.topic);
        buffer.append("\t success: " + this.success);
        return buffer.toString();
    }
}