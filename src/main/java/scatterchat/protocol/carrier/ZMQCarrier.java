package scatterchat.protocol.carrier;

import org.zeromq.ZMQ;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;
import scatterchat.protocol.message.info.ServeTopicRequest;
import scatterchat.protocol.message.info.ServeTopicResponse;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public final class ZMQCarrier {

    private static final Map<MessageType, Function<byte[], Message>> deserializers = new HashMap<>();

    static {
        deserializers.put(MessageType.CHAT_MESSAGE, ChatMessage::deserialize);
        deserializers.put(MessageType.SERVER_STATE_REQUEST, ServerStateRequest::deserialize);
        deserializers.put(MessageType.SERVER_STATE_RESPONSE, ServerStateResponse::deserialize);
        deserializers.put(MessageType.SERVE_TOPIC_REQUEST, ServeTopicRequest::deserialize);
        deserializers.put(MessageType.SERVE_TOPIC_RESPONSE, ServeTopicResponse::deserialize);
        deserializers.put(MessageType.TOPIC_ENTER_MESSAGE, TopicEnterMessage::deserialize);
        deserializers.put(MessageType.TOPIC_EXIT_MESSAGE, TopicExitMessage::deserialize);
        deserializers.put(MessageType.USERS_ORSET_MESSAGE, UserORSetMessage::deserialize);
    }

    private ZMQ.Socket socket;

    public ZMQCarrier(ZMQ.Socket socket) {
        this.socket = socket;
    }

    public void register(ZMQ.Socket socket) {
        this.socket = socket;
    }

    public void sendMessage(Message message) {
        socket.sendMore(message.getType().name());
        socket.send(message.serialize());
    }

    public Message receiveMessage() {
        MessageType type = MessageType.valueOf(new String(socket.recv(0)));
        return deserializers.get(type).apply(socket.recv(0));
    }

    public void sendMessageWithTopic(Message message) {
        socket.sendMore(message.getTopic());
        sendMessage(message);
    }

    public Message receiveMessageWithTopic() {
        socket.recv(0);
        return receiveMessage();
    }

    public void sendCausalMessage(CausalMessage message) {
        socket.send(message.serialize());
    }

    public CausalMessage receiveCausalMessage() {
        return CausalMessage.deserialize(socket.recv(0));
    }

    public void sendCausalMessageWithTopic(CausalMessage message) {
        socket.sendMore(message.getMessage().getTopic());
        sendCausalMessage(message);
    }

    public CausalMessage receiveCausalMessageWithTopic() {
        socket.recv(0);
        return receiveCausalMessage();
    }

    public void sendMessageWithIdentity(String identity, Message message) {
        socket.sendMore(identity.getBytes(ZMQ.CHARSET));
        socket.sendMore("".getBytes(ZMQ.CHARSET));
        sendMessage(message);
    }

    public Message receiveMessageWithIdentity() {
        socket.recv(0);
        socket.recv(0);
        return receiveMessage();
    }
}