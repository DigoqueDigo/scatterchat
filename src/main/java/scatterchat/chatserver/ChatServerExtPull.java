package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.crdt.ORSet;
import scatterchat.crdt.ORSetAction;
import scatterchat.crdt.ORSetAction.Operation;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;

import java.util.concurrent.BlockingQueue;


public class ChatServerExtPull implements Runnable {

    private State state;
    private JSONObject config;
    private ZContext context;
    private BlockingQueue<Message> broadcast;


    public ChatServerExtPull(JSONObject config, ZContext context, State state, BlockingQueue<Message> broadcast) {
        this.state = state;
        this.config = config;
        this.context = context;
        this.broadcast = broadcast;
    }


    private void handleChatMessage(ChatMessage message) throws InterruptedException {
        this.broadcast.add(message);
    }


    private void handleTopicEnterMessage(TopicEnterMessage message) throws InterruptedException {

        synchronized (this.state) {

            String topic = message.getTopic();
            String sender = message.getSender();
            String receiver = message.getReceiver();

            ORSet orSet = state.getUsersORSetOf(topic);
            ORSetAction orSetAction = orSet.prepare(Operation.ADD, sender);
            UserORSetMessage userORSetMessage = new UserORSetMessage(sender, receiver, topic, orSetAction);

            broadcast.put(userORSetMessage);
        }
    }


    private void handleTopicExitMessage(TopicExitMessage message) throws InterruptedException {

        synchronized (this.state) {

            String topic = message.getTopic();
            String sender = message.getSender();
            String receiver = message.getReceiver();

            ORSet orSet = state.getUsersORSetOf(topic);
            ORSetAction orSetAction = orSet.prepare(Operation.REMOVE, sender);
            UserORSetMessage usersORSetMessage = new UserORSetMessage(sender, receiver, topic, orSetAction);

            broadcast.put(usersORSetMessage);
        }
    }


    @Override
    public void run() {

        try {

            Message message;
            ZMQ.Socket socket = this.context.createSocket(SocketType.PULL);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            String address = config.getString("tcpExtPull");
            socket.bind(address);

            System.out.println("[SC extPull] started");
            System.out.println("[SC extPull] bind: " + address);

            while ((message = carrier.receiveMessage()) != null) {

                System.out.println("[SC extPull] received: " + message);

                switch (message) {
                    case ChatMessage m -> handleChatMessage(m);
                    case TopicExitMessage m -> handleTopicExitMessage(m);
                    case TopicEnterMessage m -> handleTopicEnterMessage(m);
                    default -> System.out.println("[SC extPull] unknown: " + message);
                }
            }

            socket.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}