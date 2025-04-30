package scatterchat.protocol.carrier;

import org.zeromq.ZMQ;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.aggr.Aggr;
import scatterchat.protocol.message.aggr.AggrRep;
import scatterchat.protocol.message.aggr.AggrReq;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;
import scatterchat.protocol.message.cyclon.CyclonError;
import scatterchat.protocol.message.cyclon.CyclonMessage;
import scatterchat.protocol.message.cyclon.CyclonOk;
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
        deserializers.put(MessageType.CYCLON, CyclonMessage::deserialize);
        deserializers.put(MessageType.CYCLON_OK, CyclonOk::deserialize);
        deserializers.put(MessageType.CYCLON_ERROR, CyclonError::deserialize);
        deserializers.put(MessageType.AGGR, Aggr::deserialize);
        deserializers.put(MessageType.AGGR_REQ, AggrReq::deserialize);
        deserializers.put(MessageType.AGGR_REP, AggrRep::deserialize);
    }

    private ZMQ.Socket socket;

    public ZMQCarrier(ZMQ.Socket socket) {
        this.socket = socket;
    }

    public void sendMessage(Message message) {
        socket.sendMore(message.getType().name());
        socket.send(message.serialize());
    }

    public Message receiveMessage() {
        byte[] header = socket.recv(0);

        if (header == null){
            return null;
        }

        byte[] payload = socket.recv(0);
        MessageType type = MessageType.valueOf(new String(header));
        return deserializers.get(type).apply(payload);
    }

    public void sendMessageWithTopic(String topic, Message message) {
        socket.sendMore(topic);
        sendMessage(message);
    }

    public Message receiveMessageWithTopic() {
        byte[] topic = socket.recv(0);
        return (topic == null) ? null : receiveMessage();
    }

    public void sendCausalMessage(CausalMessage message) {
        socket.send(message.serialize());
    }

    public CausalMessage receiveCausalMessage() {
        byte[] payload = socket.recv(0);
        return (payload == null) ? null : CausalMessage.deserialize(payload);
    }

    public void sendCausalMessageWithTopic(String topic, CausalMessage message) {
        socket.sendMore(topic);
        sendCausalMessage(message);
    }

    public CausalMessage receiveCausalMessageWithTopic() {
        byte[] topic = socket.recv(0);
        return (topic == null) ? null : receiveCausalMessage();
    }
}