package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.crdt.ORSet;
import scatterchat.clock.VectorClock;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.info.ServeTopicRequest;
import scatterchat.protocol.message.info.ServeTopicResponse;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;


public class ChatServerExtRep implements Runnable {

    private State state;
    private JSONObject config;
    private ZContext context;
    private BlockingQueue<Message> broadcast;


    public ChatServerExtRep(JSONObject config, ZContext context, State state, BlockingQueue<Message> broadcast) {
        this.state = state;
        this.config = config;
        this.context = context;
        this.broadcast = broadcast;
    }


    private void handleServeTopicRequest(ServeTopicRequest message, ZMQCarrier carrier) throws InterruptedException {

        synchronized (this.state) {

            String topic = message.getTopic();
            ChatServerEntry nodeId = this.state.getNodeId();

            if (!this.state.hasTopic(topic)) {
                this.state.registerUsersORSet(topic, new ORSet(nodeId));
                this.state.registerServerNodes(topic, message.getNodes());
                this.state.registerVectorClock(topic, new VectorClock(nodeId, message.getNodes()));
            }

            ServeTopicResponse response = new ServeTopicResponse(
                nodeId.repAddress(),
                message.getSender(),
                topic,
                true
            );

            // TODO :: FALTA FAZER BROADCAST PARA A FRENTE
        //    broadcast.put(message);
            carrier.sendMessage(response);
            System.out.println("[SC extRep] send: " + response);
        }
    }


    private void handleServerStateRequest(ServerStateRequest message, ZMQCarrier carrier) {

        synchronized (this.state) {

            ChatServerEntry nodeId = this.state.getNodeId();
            Map<String, Set<String>> serverState = this.state.getState();

            ServerStateResponse response = new ServerStateResponse(
                nodeId.repAddress(),
                message.getSender(),
                serverState
            );

            carrier.sendMessage(response);
            System.out.println("[SC extRep] send: " + response);
        }
    }


    @Override
    public void run() {

        try {

            ZMQ.Socket socket = this.context.createSocket(SocketType.REP);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            String bindAddress = config.getString("tcpExtRep");
            socket.bind(bindAddress);

            System.out.println("[SC extRep] started");            
            System.out.println("[SC extRep] bind: " + bindAddress);

            Message message;

            while ((message = carrier.receiveMessage()) != null) {

                System.out.println("[SC extRep] received: " + message);

                switch (message) {
                    case ServeTopicRequest m -> handleServeTopicRequest(m, carrier);
                    case ServerStateRequest m -> handleServerStateRequest(m, carrier);
                    default -> System.out.println("[SC extRep] unknown: " + message);
                }
            }

            socket.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}