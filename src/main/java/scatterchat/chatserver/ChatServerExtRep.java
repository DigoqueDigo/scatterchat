package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.clock.VectorClock;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.info.ServeTopicRequest;
import scatterchat.protocol.message.info.ServeTopicResponse;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;

import java.util.concurrent.BlockingQueue;


public class ChatServerExtRep implements Runnable {

    private State state;
    private JSONObject config;
    private BlockingQueue<Message> broadcast;


    public ChatServerExtRep(JSONObject config, State state, BlockingQueue<Message> broadcast) {
        this.state = state;
        this.config = config;
        this.broadcast = broadcast;
    }


    private void handleServeTopicRequest(ServeTopicRequest message, ZMQCarrier carrier) throws InterruptedException {

        synchronized (state){
            final String topic = message.getTopic();
            state.addUsersORSetOf(topic);
            state.setNodesOf(topic, message.getNodes());
            state.setVectorClockOf(topic, new VectorClock(message.getNodes()));
        }

        broadcast.put(message);
        ServeTopicResponse response = new ServeTopicResponse(message.getTopic(), true);
        carrier.sendMessage(response);
    }


    private void handleServerStateRequest(ServerStateRequest message, ZMQCarrier carrier) {
        synchronized (state) {
            ServerStateResponse response = new ServerStateResponse(state.getState());
            carrier.sendMessage(response);
        }
    }


    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.REP);

            String address = config.getString("extRepTCPAddress");
            socket.bind(address);

            Message message = null;
            ZMQCarrier carrier = new ZMQCarrier(socket);

            System.out.println("[SC extRep] started on: " + address);

            while ((message = carrier.receiveMessage()) != null) {

                System.out.println("[SC extRep] Received: " + message);

                switch (message) {
                    case ServeTopicRequest m -> handleServeTopicRequest(m, carrier);
                    case ServerStateRequest m -> handleServerStateRequest(m, carrier);
                    default -> System.out.println("[SC extRep] Unknown: " + message);
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