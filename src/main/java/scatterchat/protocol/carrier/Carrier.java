package scatterchat.protocol.carrier;

import org.zeromq.ZMQ;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.Message.MessageType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public final class Carrier {

    private final Map<MessageType, Function<byte[], Message>> deserializers;
    private ZMQ.Socket socket;

    public Carrier(ZMQ.Socket socket) {
        this.socket = socket;
        this.deserializers = new HashMap<>();
    }

    public void register(ZMQ.Socket socket) {
        this.socket = socket;
    }

    public void on(MessageType type, Function<byte[], Message> deserializer) {
        this.deserializers.put(type, deserializer);
    }

    public void sendMessage(Message message) {
        socket.sendMore(message.getType().name());
        socket.send(message.serialize());
    }

    public Message receiveMessage() {
        MessageType type = MessageType.valueOf(new String(socket.recv()));
        return deserializers.get(type).apply(socket.recv());
    }

    public void sendMessageWithTopic(Message message) {
        socket.sendMore(message.getTopic());
        sendMessage(message);
    }

    public Message receiveMessageWithTopic() {
        socket.recv();
        return receiveMessage();
    }

    public void sendCausalMessage(CausalMessage message) {
        socket.send(message.serialize());
    }

    public CausalMessage receiveCausalMessage() {
        return CausalMessage.deserialize(socket.recv());
    }

    public void sendCausalMessageWithTopic(CausalMessage message) {
        socket.sendMore(message.getMessage().getTopic());
        sendCausalMessage(message);
    }

    public CausalMessage receiveCausalMessageWithTopic() {
        socket.recv();
        return receiveCausalMessage();
    }
}