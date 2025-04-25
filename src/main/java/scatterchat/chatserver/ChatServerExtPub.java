package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.crdt.ORSet;
import scatterchat.crdt.ORSetAction;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;

import java.util.concurrent.BlockingQueue;


public class ChatServerExtPub implements Runnable {

    private State state;
    private JSONObject config;
    private ZContext context;
    private BlockingQueue<Message> delivered;


    public ChatServerExtPub(JSONObject config, ZContext context, State state, BlockingQueue<Message> delivered) {
        this.state = state;
        this.config = config;
        this.context = context;
        this.delivered = delivered;
    }


    private void handleChatMessage(ChatMessage message, ZMQCarrier carrier) {
        carrier.sendMessageWithTopic(message.getTopic(), message);
    }


    private void handleUsersORSetMessage(UserORSetMessage message) {
        synchronized (this.state) {
            ORSet orSet = state.getUsersORSetOf(message.getTopic());
            ORSetAction orSetAction = message.getORSetAction();
            orSet.effect(orSetAction);
            System.out.println(this.state);
        }
    }


    @Override
    public void run() {

        try{

            Message message;
            ZMQ.Socket socket = this.context.createSocket(SocketType.PUB);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            String bindAddress = config.getString("tcpExtPub");
            socket.bind(bindAddress);

            System.out.println("[SC extPub] started");
            System.out.println("[SC extPub] bind: " + bindAddress);

            while ((message = this.delivered.take()) != null) {

                System.out.println("[SC extPub] received: " + message);

                switch (message) {
                    case ChatMessage m -> handleChatMessage(m, carrier);
                    case UserORSetMessage m -> handleUsersORSetMessage(m);
                    default -> System.out.println("[SC extPuB] unknown: " + message);
                }
            }

            socket.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}