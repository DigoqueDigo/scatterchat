package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.clock.VectorClock;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.UsersORSetMessage;
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


    private void forwardAsCausalMessage(Message message, Carrier carrier) {

        synchronized (state){

            final String topic = message.getTopic();
            final String nodeId = state.getNodeId();

            message.setSender(nodeId);
            VectorClock vectorClock = state.getVectorClockOf(topic);
            CausalMessage causalMessage = new CausalMessage(message, vectorClock);

            carrier.sendCausalMessageWithTopic(causalMessage);
            vectorClock.putTimeOf(nodeId, vectorClock.getTimeOf(nodeId) + 1);
            state.setVectorClockOf(topic, vectorClock);
        }
    }


    private void handleServeTopicRequest(ServeTopicRequest message, Carrier carrier){
        message.setTopic("[internal]" + message.getTopic());
        CausalMessage causalMessage = new CausalMessage(message, null);
        carrier.sendCausalMessageWithTopic(causalMessage);
    }


    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PUB);

            final String address = config.getString("interPubAddress");
            socket.bind(address);
            System.out.println("[SC interPub] started on: " + address);

            Message message = null;
            Carrier carrier = new Carrier(socket);

            while ((message = broadcast.take()) != null) {

                System.out.println("[SC interPub] Reveived: " + message.toString());

                switch (message) {
                    case ChatMessage m -> forwardAsCausalMessage(m, carrier);
                    case UsersORSetMessage m -> forwardAsCausalMessage(m, carrier);
                    case ServeTopicRequest m -> handleServeTopicRequest(m, carrier);
                    default -> System.out.println("[SC interPub] Unknown: " + message);
                }
            }

            socket.close();
            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}