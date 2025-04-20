package scatterchat.protocol.message.cyclon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;


public class CyclonMessage extends Message {

    private String identity;
    private Set<CyclonEntry> subSet;


    public CyclonMessage(){
        super(MessageType.CYCLON);
        this.subSet = null;
    }


    public CyclonMessage(String identity, String sender, Set<CyclonEntry> subSet) {
        super(MessageType.CYCLON, null, sender);
        this.identity = identity;
        this.subSet = new HashSet<>(subSet);
    }


    public String getIdentity() {
        return this.identity;
    }


    public List<CyclonEntry> getSubSet() {
        return new ArrayList<>(this.subSet);
    }


    public byte[] serialize() {

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(CyclonMessage.class);
        kryo.register(HashSet.class);
        kryo.register(CyclonEntry.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public static CyclonMessage deserialize(byte[] data) {

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(CyclonMessage.class);
        kryo.register(HashSet.class);
        kryo.register(CyclonEntry.class);

        CyclonMessage cyclonMessage = kryo.readObject(input, CyclonMessage.class);
        input.close();

        return cyclonMessage;
    }


    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.toString());
        buffer.append("\t subset: " + this.subSet);
        return buffer.toString();
    }
}
