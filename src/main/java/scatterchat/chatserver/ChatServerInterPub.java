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
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.crtd.UserORSetMessage;
import scatterchat.protocol.message.info.ServeTopicRequest;

import java.util.concurrent.BlockingQueue;


public class ChatServerInterPub implements Runnable {

    private State state;
    private JSONObject config;
    private ZContext context;
    private BlockingQueue<Message> broadcast;


    public ChatServerInterPub(JSONObject config, ZContext context, State state, BlockingQueue<Message> broadcast) {
        this.state = state;
        this.config = config;
        this.context = context;
        this.broadcast = broadcast;
    }


    private void forwardAsCausal(Message message, String topic, ZMQCarrier carrier) {

        ChatServerEntry nodeId = state.getNodeId();
        VectorClock vectorClock = state.getVectorClockOf(topic);

        message.setSender(nodeId.interPubAddress());
        vectorClock.putTimeOf(nodeId, vectorClock.getTimeOf(nodeId) + 1);
        CausalMessage causalMessage = new CausalMessage(topic, message, vectorClock);

        this.state.setVectorClockOf(topic, vectorClock);
        carrier.sendCausalMessageWithTopic(topic, causalMessage);
    }


    private void handleChatMessage(ChatMessage message, ZMQCarrier carrier) {
        synchronized (this.state) {
            forwardAsCausal(message, message.getTopic(), carrier);
        }
    }


    private void handleUserORSetMessage(UserORSetMessage message, ZMQCarrier carrier) {
        synchronized (this.state) {
            forwardAsCausal(message, message.getTopic(), carrier);
        }
    }


    private void handleServeTopicRequest(ServeTopicRequest message, ZMQCarrier carrier){
        synchronized (this.state) {
            String internalTopic = this.config.getString("internalTopic");
            CausalMessage causalMessage = new CausalMessage(message.getTopic(), message, null);
            carrier.sendCausalMessageWithTopic(internalTopic, causalMessage);
        }
    }


    @Override
    public void run() {

        try {

            ZMQ.Socket socket = this.context.createSocket(SocketType.PUB);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            String tcpAddress = config.getString("tcpInterPub");
            String inprocAddress = config.getString("inprocPubSub");
            
            socket.bind(tcpAddress);
            socket.bind(inprocAddress);
            
            System.out.println("[SC interPub] started");
            System.out.println("[SC interPub] bind: " + tcpAddress);
            System.out.println("[SC interPub] bind: " + inprocAddress);
            
            Message message;

            while ((message = broadcast.take()) != null) {

                System.out.println("[SC interPub] received: " + message.toString());

                switch (message) {
                    case ChatMessage m -> handleChatMessage(m, carrier);
                    case UserORSetMessage m -> handleUserORSetMessage(m, carrier);
                    case ServeTopicRequest m -> handleServeTopicRequest(m, carrier);
                    default -> System.out.println("[SC interPub] unknown: " + message);
                }
            }

            socket.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}