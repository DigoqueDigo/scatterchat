package scatterchat.chatserver;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.info.ServeTopicRequest;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class ChatServerExtRep implements Runnable {

    private State state;
    private String extRepAddress;


    public ChatServerExtRep(State state, String extRepAddress) {
        this.state = state;
        this.extRepAddress = extRepAddress;
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


    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind(extRepAddress);

            Message message = null;
            Carrier carrier = new Carrier(socket);

            carrier.on(MessageType.SERVE_TOPIC_REQUEST, ServeTopicRequest::deserialize);
            carrier.on(MessageType.SERVER_STATE_REQUEST, ServerStateRequest::deserialize);
            System.out.println("[SC extRep] started on: " + extRepAddress);

            while ((message = carrier.receiveMessage()) != null) {

                System.out.println("[SC extRep] Received: " + message);

                switch (message) {
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