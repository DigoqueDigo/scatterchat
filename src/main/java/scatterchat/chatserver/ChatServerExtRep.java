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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;


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

            System.out.println(this.state);
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

            broadcast.put(message);
            carrier.sendMessage(response);
        }
    }


    private void handleServerStateRequest(ServerStateRequest message, ZMQCarrier carrier) {

        synchronized (this.state) {

            ChatServerEntry nodeId = this.state.getNodeId();
            Map<String, Set<String>> totalState = this.state.getTotalState();
            Map<String, Set<String>> localState = this.state.getLocalState();

            ServerStateResponse response = new ServerStateResponse(
                nodeId.repAddress(),
                message.getSender(),
                totalState,
                localState
            );

            carrier.sendMessage(response);
        }
    }


    @Override
    public void run() {
        try {
            ZMQ.Socket frontend = this.context.createSocket(SocketType.ROUTER);
            ZMQ.Socket backend = this.context.createSocket(SocketType.DEALER);

            int totalWorkers = config.getInt("totalWorkers");
            String bindAddress = config.getString("tcpExtRep");
            String proxyAddress = config.getString("inprocProxy");

            frontend.bind(bindAddress);
            backend.bind(proxyAddress);

            System.out.println("[SC extRep] started");
            System.out.println("[SC extRep] bind: " + bindAddress);
            System.out.println("[SC extRep] bind: " + proxyAddress);

            Runnable workerTask = () -> {
                try {
                    Message message;
                    ZMQ.Socket socket = context.createSocket(SocketType.REP);
                    ZMQCarrier carrier = new ZMQCarrier(socket);

                    socket.connect(proxyAddress);
                    String identity = String.format("[SC extRep worker %s]", Thread.currentThread().threadId());

                    System.out.println(identity + " stated");
                    System.out.println(identity + "connect: " + proxyAddress);

                    while ((message = carrier.receiveMessage()) != null) {
                        System.out.println(identity + " received: " + message);
                        switch (message) {
                            case ServeTopicRequest m -> handleServeTopicRequest(m, carrier);
                            case ServerStateRequest m -> handleServerStateRequest(m, carrier);
                            default -> System.out.println(identity + " unknown: " + message);
                        }
                    }

                    socket.close();
                    System.out.println(identity + " close connection");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

            ThreadFactory threadFactory = Thread.ofVirtual().factory();
            List<Thread> workers = IntStream.range(0, totalWorkers)
                .mapToObj(x -> threadFactory.newThread(workerTask))
                .toList();

            workers.forEach(worker -> worker.start());
            ZMQ.proxy(frontend, backend, null);

            for (Thread worker : workers) {
                worker.join();
            }

            frontend.close();
            backend.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}