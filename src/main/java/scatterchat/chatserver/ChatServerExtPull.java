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

    private final State state;
    private final JSONObject config;
    private final BlockingQueue<Message> broadcast;


    public ChatServerExtPull(JSONObject config, State state, BlockingQueue<Message> broadcast) {
        this.state = state;
        this.config = config;
        this.broadcast = broadcast;
    }


    private void handleChatMessage(ChatMessage message) throws InterruptedException {
        this.broadcast.add(message);
    }


    private void handleTopicEnterMessage(TopicEnterMessage message) throws InterruptedException{

        synchronized (state){

            final String topic = message.getTopic();
            final String sender = message.getSender();

            ORSet orSet = state.getUsersORSetOf(topic);
            ORSetAction orSetAction = orSet.prepare(Operation.ADD, sender);
            UserORSetMessage userORSetMessage = new UserORSetMessage(sender, topic, orSetAction);

            orSet.effect(orSetAction);
            broadcast.put(userORSetMessage);
        }
    }


    private void handleTopicExitMessage(TopicExitMessage message) throws InterruptedException {

        synchronized (state) {

            final String topic = message.getTopic();
            final String sender = message.getSender();

            ORSet orSet = state.getUsersORSetOf(topic);
            ORSetAction orSetAction = orSet.prepare(Operation.REMOVE, sender);
            UserORSetMessage usersORSetMessage = new UserORSetMessage(sender, topic, orSetAction);

            orSet.effect(orSetAction);
            broadcast.put(usersORSetMessage);
        }
    }


    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PULL);

            String address = config.getString("extPullTCPAddress");
            socket.bind(address);

            Message message = null;
            ZMQCarrier carrier = new ZMQCarrier(socket);

            System.out.println("[SC extPull] started on: " + address);

            while ((message = carrier.receiveMessage()) != null) {

                System.out.println("[SC extPull] Received: " + message);

                switch (message) {
                    case ChatMessage m -> handleChatMessage(m);
                    case TopicExitMessage m -> handleTopicExitMessage(m);
                    case TopicEnterMessage m -> handleTopicEnterMessage(m);
                    default -> System.out.println("[SC extPull] Unknown: " + message);
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