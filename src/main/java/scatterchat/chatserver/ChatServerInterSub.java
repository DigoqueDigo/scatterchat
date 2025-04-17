package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;
import scatterchat.protocol.message.info.ServeTopicRequest;

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

            final String topic = message.getTopic().replace("[internal]", "");
            subSocket.subscribe(topic);

            for (String nodeInterPubAddres : message.getNodes()) {
                subSocket.connect(nodeInterPubAddres);
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

            while ((causalMessage = carrier.receiveCausalMessageWithTopic()) != null) {

                System.out.println("[SC interSub] Received: " + causalMessage.getMessage());

                switch (causalMessage.getMessage()) {
                    case ChatMessage m -> forwardCausalMessage(causalMessage);
                    case UserORSetMessage m -> forwardCausalMessage(causalMessage);
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