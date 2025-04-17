package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.ORSet;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.chat.ChatMessage;
import scatterchat.protocol.messages.crtd.ORSetMessage;
import scatterchat.protocol.messages.crtd.UsersORSetMessage;

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
        carrier.sendWithTopic(message);
    }

    private void handleUsersORSetMessage(UsersORSetMessage message) {
        synchronized (state) {
            ORSet orSet = state.getUsersORSetOf(message.getTopic());
            ORSetMessage orSetMessage = message.getOrSetMessage();
            orSet.effect(orSetMessage);
        }
    }

    @Override
    public void run() {
        try{
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PUB);

            final String address = config.getString("extPubAddress");
            socket.bind(address);

            Message message = null;
            Carrier carrier = new Carrier(socket);

            System.out.println("[SC extPub] started on: " + address);

            while ((message = this.delivered.take()) != null) {

                System.out.println("[SC extPub] Received: " + message);

                switch (message) {
                    case ChatMessage m -> handleChatMessage(m, carrier);
                    case UsersORSetMessage m -> handleUsersORSetMessage(m);
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