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

    private void handleServeTopicRequest(ServeTopicRequest message, ZMQ.Socket socket) {

        synchronized (state) {

            final String topic = message.getTopic().replace("[internal]", "");
            socket.subscribe(topic);

            for (String nodeInterPubAddres : message.getNodes()) {
                socket.connect(nodeInterPubAddres);
            }
        }
    }

    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.SUB);

            String tcpAddress = config.getString("interPubTCPAddress");
            String inprocAddress = config.getString("interPubProcAddress");

            socket.connect(tcpAddress);
            socket.connect(inprocAddress);
            socket.subscribe("[internal]");

            CausalMessage causalMessage = null;
            Carrier carrier = new Carrier(socket);

            System.out.println("[SC interSub] started");

            while ((causalMessage = carrier.receiveCausalMessageWithTopic()) != null) {

                System.out.println("[SC interSub] Received: " + causalMessage.getMessage());

                switch (causalMessage.getMessage()) {
                    case ChatMessage m -> forwardCausalMessage(causalMessage);
                    case UserORSetMessage m -> forwardCausalMessage(causalMessage);
                    case ServeTopicRequest m -> handleServeTopicRequest(m, socket);
                    default -> System.out.println("[SC interSub] Unknown: " + causalMessage.getMessage());
                }
            }

            socket.close();
            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}