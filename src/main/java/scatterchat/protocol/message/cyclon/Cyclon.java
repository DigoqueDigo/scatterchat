package scatterchat.protocol.message.cyclon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;


public class Cyclon extends Message {

    private Set<String> subSet;

    public Cyclon(){
        super(MessageType.CYCLON);
        this.subSet = null;
    }

    public Cyclon(Set<String> subSet) {
        super(MessageType.CYCLON);
        this.subSet = new HashSet<>(subSet);
    }

    public Set<String> getSubSet() {
        return this.subSet;
    }

    public byte[] serialize() {

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(Cyclon.class);
        kryo.register(HashSet.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static Cyclon deserialize(byte[] data) {

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(Cyclon.class);
        kryo.register(HashSet.class);

        Cyclon cyclon = kryo.readObject(input, Cyclon.class);
        input.close();

        return cyclon;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.toString());
        buffer.append("\t subset: " + this.subSet);
        return buffer.toString();
    }
}
