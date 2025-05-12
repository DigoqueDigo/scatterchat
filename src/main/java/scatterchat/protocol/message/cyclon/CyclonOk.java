package scatterchat.protocol.message.cyclon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;


public class CyclonOk extends Message {

    private List<CyclonEntry> subSet;

    public CyclonOk(){
        super(MessageType.CYCLON_OK);
        this.subSet = null;
    }

    public CyclonOk(String sender, String receiver, List<CyclonEntry> subSet) {
        super(MessageType.CYCLON_OK, sender, receiver);
        this.subSet = new ArrayList<>(subSet);
    }

    public List<CyclonEntry> getSubSet() {
        return new ArrayList<>(this.subSet);
    }

    public byte[] serialize() {

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(CyclonOk.class);
        kryo.register(ArrayList.class);
        kryo.register(CyclonEntry.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static CyclonOk deserialize(byte[] data) {

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(CyclonOk.class);
        kryo.register(ArrayList.class);
        kryo.register(CyclonEntry.class);

        CyclonOk cyclonOk = kryo.readObject(input, CyclonOk.class);
        input.close();

        return cyclonOk;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.toString());
        buffer.append(", ").append(this.subSet);
        return buffer.toString();
    }
}