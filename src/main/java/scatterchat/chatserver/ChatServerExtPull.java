package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.crdt.ORSet;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.chat.ChatExitMessage;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.ORSetMessage;
import scatterchat.protocol.message.crtd.UsersORSetMessage;
import scatterchat.protocol.message.crtd.ORSetMessage.Operation;

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

        synchronized (state) {

            final String topic = message.getTopic();
            final String sender = message.getSender();
            ORSet orSet = state.getUsersORSetOf(topic);

            if (!orSet.contains(sender)) {

                ORSetMessage orSetMessage = orSet.prepare(Operation.ADD, sender);
                UsersORSetMessage usersORSetMessage = new UsersORSetMessage(topic, sender, orSetMessage);

                orSet.effect(orSetMessage);
                broadcast.put(usersORSetMessage);
            }

            broadcast.put(message);
        }
    }

    private void handleChatExitMessage(ChatExitMessage message) throws InterruptedException {

        synchronized (state) {

            final String topic = message.getTopic();
            final String sender = message.getSender();

            ORSet orSet = state.getUsersORSetOf(topic);
            ORSetMessage orSetMessage = orSet.prepare(Operation.REMOVE, sender);
            UsersORSetMessage usersORSetMessage = new UsersORSetMessage(topic, sender, orSetMessage);

            orSet.effect(orSetMessage);
            broadcast.put(usersORSetMessage);
        }
    }

    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PULL);

            final String address = config.getString("extPullAddress");
            socket.bind(address);

            Message message = null;
            Carrier carrier = new Carrier(socket);
            System.out.println("[SC extPull] started on: " + address);

            carrier.on(MessageType.CHAT_MESSAGE, ChatMessage::deserialize);
            carrier.on(MessageType.CHAT_EXIT_MESSAGE, ChatExitMessage::deserialize);

            while ((message = carrier.receiveMessage()) != null) {
                System.out.println("[SC extPull] Received: " + message);

                switch (message) {
                    case ChatMessage m -> handleChatMessage(m);
                    case ChatExitMessage m -> handleChatExitMessage(m);
                    default -> System.out.println("[SC extPull] Unknown: " + message);
                }
            }

            socket.close();
            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}