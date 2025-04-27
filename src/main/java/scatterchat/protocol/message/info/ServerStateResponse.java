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

    private Map<String, Set<String>> serverTotalState;
    private Map<String, Set<String>> serverLocalState;

    public ServerStateResponse() {
        super(MessageType.SERVER_STATE_RESPONSE);
    }

    public ServerStateResponse(String sender, String receiver, Map<String, Set<String>> serverTotalState, Map<String, Set<String>> serverLocalState) {
        super(MessageType.SERVER_STATE_RESPONSE, sender, receiver);
        this.serverTotalState = serverTotalState;
        this.serverLocalState = serverLocalState;
    }

    public Map<String, Set<String>> getServerTotalState() {
        return new HashMap<>(this.serverTotalState);
    }

    public Map<String, Set<String>> getServerLocalState() {
        return new HashMap<>(this.serverLocalState);
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
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append(", ").append(this.serverTotalState);
        buffer.append(", ").append(this.serverLocalState);
        return buffer.toString();
    }
}