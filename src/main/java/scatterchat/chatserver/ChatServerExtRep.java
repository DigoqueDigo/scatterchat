package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.info.ServeTopicRequest;
import scatterchat.protocol.message.info.ServeTopicResponse;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;


public class ChatServerExtRep implements Runnable {

    private State state;
    private JSONObject config;
    private BlockingQueue<Message> broadcast;

    public ChatServerExtRep(JSONObject config, State state, BlockingQueue<Message> broadcast) {
        this.state = state;
        this.config = config;
        this.broadcast = broadcast;
    }

    private void handleServerStateRequest(ServerStateRequest message, Carrier carrier) {

        synchronized (state) {

            Map<String, Set<String>> serverState = state.getServedTopics()
                .stream()
                .collect(Collectors.toMap(
                    topic -> topic,
                    topic -> state.getUsersORSetOf(topic).elements()));

            ServerStateResponse response = new ServerStateResponse(serverState);
            carrier.sendMessage(response);
        }
    }

    private void handleServeTopicRequest(ServeTopicRequest message, Carrier carrier) throws InterruptedException {
        broadcast.put(message);
        ServeTopicResponse response = new ServeTopicResponse(true);
        carrier.sendMessage(response);
    }

    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.REP);

            final String address = config.getString("extRepAddress");
            socket.bind(address);
            System.out.println("[SC extRep] started on: " + address);

            Message message = null;
            Carrier carrier = new Carrier(socket);

            carrier.on(MessageType.SERVE_TOPIC_REQUEST, ServeTopicRequest::deserialize);
            carrier.on(MessageType.SERVER_STATE_REQUEST, ServerStateRequest::deserialize);

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}