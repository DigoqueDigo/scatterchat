package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.crdt.ORSet;
import scatterchat.crdt.ORSetAction;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;

import java.util.concurrent.BlockingQueue;


public class ChatServerExtPub implements Runnable {

    private State state;
    private JSONObject config;
    private BlockingQueue<Message> delivered;

    public ChatServerExtPub(JSONObject config, State state, BlockingQueue<Message> delivered) {
        this.state = state;
        this.config = config;
        this.delivered = delivered;
    }

    private void handleChatMessage(ChatMessage message, Carrier carrier) {
        carrier.sendMessageWithTopic(message);
    }

    private void handleUsersORSetMessage(UserORSetMessage message) {
        synchronized (state) {
            ORSet orSet = state.getUsersORSetOf(message.getTopic());
            ORSetAction orSetAction = message.getORSetAction();
            orSet.effect(orSetAction);
        }
    }

    @Override
    public void run() {
        try{
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PUB);

            final String address = config.getString("extPubAddress");
            socket.bind(address);
            System.out.println("[SC extPub] started on: " + address);

            Message message = null;
            Carrier carrier = new Carrier(socket);

            while ((message = this.delivered.take()) != null) {

                System.out.println("[SC extPub] Received: " + message);

                switch (message) {
                    case ChatMessage m -> handleChatMessage(m, carrier);
                    case UserORSetMessage m -> handleUsersORSetMessage(m);
                    default -> System.out.println("[SC extPuB] Unknown: " + message);
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