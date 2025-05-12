package scatterchat.protocol.message.cyclon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;


public class CyclonError extends Message{

    public CyclonError() {
        super(MessageType.CYCLON_ERROR);
    }

    public CyclonError(String sender, String receiver) {
        super(MessageType.CYCLON_ERROR, sender, receiver);
    }

    public byte[] serialize() {

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(CyclonError.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static CyclonError deserialize(byte[] data) {

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(CyclonError.class);

        CyclonError cyclonError = kryo.readObject(input, CyclonError.class);
        input.close();

        return cyclonError;
    }
}