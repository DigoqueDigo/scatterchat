package scatterchat.protocol.messages;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.clock.VectorClock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class CausalMessage {

    private Message message;
    private VectorClock vectorClock;

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

        kryo.register(CausalMessage.class);
        kryo.register(Message.class);
        kryo.register(VectorClock.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static CausalMessage deserialize(byte[] data) {
        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(CausalMessage.class);
        kryo.register(Message.class);
        kryo.register(VectorClock.class);

        CausalMessage causalMessage = kryo.readObject(input, CausalMessage.class);
        input.close();

        return causalMessage;
    }
}