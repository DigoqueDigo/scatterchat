package scatterchat.protocol.message.aggr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;


public class AggrReq extends Message {

    private String topic;

    public AggrReq() {
        super(MessageType.AGGR_REQ);
    }

    public AggrReq(String sender, String receiver, String topic) {
        super(MessageType.AGGR_REQ, sender, receiver);
        this.topic = topic;
    }

    public String getTopic() {
        return this.topic;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(AggrReq.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static AggrReq deserialize(byte[] data) {
        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(AggrReq.class);

        AggrReq aggrReq = kryo.readObject(input, AggrReq.class);
        input.close();

        return aggrReq;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append(", ").append(topic);
        return buffer.toString();
    }
}