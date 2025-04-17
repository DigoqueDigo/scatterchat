package scatterchat.protocol.carrier;

import org.zeromq.ZMQ;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.CausalMessage;
import scatterchat.protocol.messages.Message.MessageType;

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

    public void send(Message message) {
        socket.sendMore(message.getType().name());
        socket.send(message.serialize());
    }

    public Message receive() {
        MessageType type = MessageType.valueOf(new String(socket.recv()));
        return deserializers.get(type).apply(socket.recv());
    }

    public void sendWithTopic(Message message) {
        socket.sendMore(message.getTopic());
        send(message);
    }

    public Message receiveWithTopic() {
        socket.recv();
        return receive();
    }

    public void sendCausalWihtTopic(CausalMessage message) {
        System.out.println("Not implemented");
    }

    public CausalMessage receiveCausalWithTopic() {
        System.out.println("Not implemented");
        return null;
    }
}