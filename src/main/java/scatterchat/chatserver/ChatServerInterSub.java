package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.CausalMessage;
import scatterchat.protocol.messages.chat.ChatMessage;
import scatterchat.protocol.messages.Message.MessageType;
import scatterchat.protocol.messages.crtd.UsersORSetMessage;
import scatterchat.protocol.messages.info.ServeTopicRequest;

import java.util.concurrent.BlockingQueue;


public class ChatServerInterSub implements Runnable {

    private final State state;
    private final JSONObject config;
    private final BlockingQueue<CausalMessage> received;


    public ChatServerInterSub(JSONObject config, State state, BlockingQueue<CausalMessage> received) {
        this.state = state;
        this.config = config;
        this.received = received;
    }

    private void forwardCausalMessage(CausalMessage message) throws InterruptedException {
        this.received.put(message);
    }


    private void handleServeTopicRequest(ServeTopicRequest message, ZMQ.Socket subSocket) {

        synchronized (state) {

            final String nodeId = state.getNodeId();
            final String topic = message.getTopic().replace("[internal]", "");

            ServeTopicRequest serveTopicRequest = (ServeTopicRequest) message;
            subSocket.subscribe(topic);

            for (String nodeInterPubAddres : serveTopicRequest.getNodes()) {
                if (!nodeInterPubAddres.equals(nodeId)) {
                    subSocket.connect(nodeInterPubAddres);
                }
            }
        }
    }


    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket subSocket = context.createSocket(SocketType.SUB);

            final String address = config.getString("interPubAddress");
            subSocket.connect(address);
            subSocket.subscribe("[internal]");
            System.out.println("[SC interSub] started");

            CausalMessage causalMessage = null;
            Carrier carrier = new Carrier(subSocket);

            carrier.on(MessageType.CHAT_MESSAGE, ChatMessage::deserialize);
            carrier.on(MessageType.USERS_ORSET_MESSAGE, UsersORSetMessage::deserialize);
            carrier.on(MessageType.SERVE_TOPIC_REQUEST, ServeTopicRequest::deserialize);

            while ((causalMessage = carrier.receiveCausalWithTopic()) != null) {

                System.out.println("[SC interSub] Received: " + causalMessage.getMessage());

                switch (causalMessage.getMessage()) {
                    case ChatMessage m -> forwardCausalMessage(causalMessage);
                    case UsersORSetMessage m -> forwardCausalMessage(causalMessage);
                    case ServeTopicRequest m -> handleServeTopicRequest(m, subSocket);
                    default -> System.out.println("[SC interSub] Unknown: " + causalMessage.getMessage());
                }
            }

            subSocket.close();
            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}