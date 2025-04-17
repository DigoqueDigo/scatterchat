package scatterchat.protocol.message.info;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import scatterchat.protocol.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ServerStateResponse extends Message {

    private Map<String, Set<String>> serverState;

    public ServerStateResponse() {
        super(MessageType.SERVER_STATE_RESPONSE);
    }

    public ServerStateResponse(Map<String, Set<String>> serverState) {
        super(MessageType.SERVER_STATE_RESPONSE);
        this.serverState = serverState;
    }

    public Map<String, Set<String>> getServerState() {
        return this.serverState;
    }

    public byte[] serialize() {
        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(MessageType.class);
        kryo.register(ServerStateResponse.class);
        kryo.register(HashMap.class);
        kryo.register(HashSet.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static ServerStateResponse deserialize(byte[] data) {
        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(MessageType.class);
        kryo.register(ServerStateResponse.class);
        kryo.register(HashMap.class);
        kryo.register(HashSet.class);

        ServerStateResponse serverInfoMessage = kryo.readObject(input, ServerStateResponse.class);
        input.close();

        return serverInfoMessage;
    }

    public String toString() {
        return super.toString() + "\t serverState: " + this.serverState;
    }
}