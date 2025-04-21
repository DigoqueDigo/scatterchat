package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.clock.VectorClock;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;
import scatterchat.protocol.message.info.ServeTopicRequest;

import java.util.concurrent.BlockingQueue;


public class ChatServerInterPub implements Runnable {

    private final State state;
    private final JSONObject config;
    private final BlockingQueue<Message> broadcast;


    public ChatServerInterPub(JSONObject config, State state, BlockingQueue<Message> broadcast) {
        this.state = state;
        this.config = config;
        this.broadcast = broadcast;
    }


    private void forwardAsCausalMessage(Message message, String topic, ZMQCarrier carrier) {

        synchronized (state){
;
            String nodeId = state.getNodeId();
            VectorClock vectorClock = state.getVectorClockOf(topic);
            CausalMessage causalMessage = new CausalMessage(message, vectorClock);

            message.setSender(nodeId);
            carrier.sendCausalMessageWithTopic(topic, causalMessage);
            vectorClock.putTimeOf(nodeId, vectorClock.getTimeOf(nodeId) + 1);
            state.setVectorClockOf(topic, vectorClock);
        }
    }


    private void handleServeTopicRequest(ServeTopicRequest message, ZMQCarrier carrier){
        synchronized (state){
            CausalMessage causalMessage = new CausalMessage(message, null);
            carrier.sendCausalMessageWithTopic("[internal]", causalMessage);
        }
    }


    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PUB);

            String tcpAddress = config.getString("tcpInterPub");
            String inprocAddress = config.getString("inprocPubSub");

            socket.bind(tcpAddress);
            socket.bind(inprocAddress);

            Message message = null;
            ZMQCarrier carrier = new ZMQCarrier(socket);

            System.out.println("[SC interPub] started on: " + tcpAddress);
            System.out.println("[SC interPub] started on: " + inprocAddress);

            while ((message = broadcast.take()) != null) {

                System.out.println("[SC interPub] Reveived: " + message.toString());

                switch (message) {
                    case ChatMessage m -> forwardAsCausalMessage(m, m.getTopic(), carrier);
                    case UserORSetMessage m -> forwardAsCausalMessage(m, m.getTopic(), carrier);
                    case ServeTopicRequest m -> handleServeTopicRequest(m, carrier);
                    default -> System.out.println("[SC interPub] Unknown: " + message);
                }
            }

            socket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}