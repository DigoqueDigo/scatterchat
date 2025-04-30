package scatterchat.client;

import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.signal.Signal;
import scatterchat.protocol.signal.TimeoutSignal;


public class ClientSub implements Runnable{

    private static final int TIMEOUT = 1_000; 

    private JSONObject config;
    private ZContext context;
    private BlockingQueue<Signal> signals;


    public ClientSub(JSONObject config, ZContext context, BlockingQueue<Signal> signals) {
        this.config = config;
        this.context = context;
        this.signals = signals;
    }


    private void handleChatMessage(ChatMessage message) {
        if (!message.getClient().equals(this.config.getString("username"))) {
            System.out.println(message.getClient() + " >>> " + message.getMessage());
        } else {
            System.out.println("[Client UI] ignored: " + message);
        }
    }


    private void handleTopicEnterMessage(TopicEnterMessage message, ZMQ.Socket socket) {
        ChatServerEntry chatServerEntry = message.getChatServerEntry();
        socket.connect(chatServerEntry.extPubAddress());
        socket.subscribe(message.getTopic());

        System.out.println("[Client SUB] connect: " + chatServerEntry.extPubAddress());
        System.out.println("[Client SUB] subcribe: " + message.getTopic());
    }


    private void handleTopicExitMessage(TopicExitMessage message, ZMQ.Socket socket) throws NullPointerException {

        if (message.getTopic() == null){
            throw new NullPointerException("client exit");
        }

        ChatServerEntry chatServerEntry = message.getChatServerEntry();
        socket.unsubscribe(message.getTopic());
        socket.disconnect(chatServerEntry.extPubAddress());

        System.out.println("[Client SUB] unsubcribe: " + message.getTopic());
        System.out.println("[Client SUB] disconnect: " + chatServerEntry.extPubAddress());
    }


    @Override
    public void run() {

        ZMQ.Socket socket = this.context.createSocket(SocketType.SUB);
        ZMQCarrier carrier = new ZMQCarrier(socket);

        String inprocAddres = config.getString("inprocPubSub");
        String internalTopic = config.getString("internalTopic");

        socket.connect(inprocAddres);
        socket.subscribe(internalTopic.getBytes(ZMQ.CHARSET));
        socket.setReceiveTimeOut(TIMEOUT);

        System.out.println("[Client SUB] connect: " + inprocAddres);
        System.out.println("[Client SUB] subscribe: " + internalTopic);

        try{

            while (true) {

                Message message = carrier.receiveMessageWithTopic();
                System.out.println("[Client SUB] received: " + message);

                if (message == null) {
                    TimeoutSignal timeoutSignal = new TimeoutSignal();
                    this.signals.put(timeoutSignal);
                } else {
                    switch (message){
                        case ChatMessage m -> handleChatMessage(m);
                        case TopicExitMessage m -> handleTopicExitMessage(m, socket);
                        case TopicEnterMessage m -> handleTopicEnterMessage(m, socket);
                        default -> System.out.println("[Client SUB] unknown: " + message);
                    }
                }
            }
        }

        catch (NullPointerException e){
            socket.unsubscribe("[internal]");
            socket.close();
            System.out.println("[Client SUB] exit");
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}