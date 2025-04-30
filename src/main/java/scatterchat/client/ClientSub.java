package scatterchat.client;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.signal.Signal;
import scatterchat.protocol.signal.TimeoutSignal;


public class ClientSub implements Runnable{

    private static final int TIMEOUT = 2_000; 

    private ZContext context;
    private JSONObject config;
    private boolean isConnected;
    private BlockingQueue<Signal> signals;


    public ClientSub(JSONObject config, ZContext context, BlockingQueue<Signal> signals) {
        this.config = config;
        this.context = context;
        this.signals = signals;
        this.isConnected = false;
    }


    private void handleChatMessage(ChatMessage message) {
        if (!message.getClient().equals(this.config.getString("username"))) {
            System.out.println(message.getClient() + " >>> " + message.getMessage());
        } else {
            System.out.println("[Client UI] ignored: " + message);
        }
    }


    private void handleTopicEnterMessage(TopicEnterMessage message, ZMQ.Socket socket) {
        this.isConnected = true;
        ChatServerEntry chatServerEntry = message.getChatServerEntry();
        socket.connect(chatServerEntry.extPubAddress());
        socket.subscribe(message.getTopic());

        System.out.println("[Client Sub] connect: " + chatServerEntry.extPubAddress());
        System.out.println("[Client Sub] subcribe: " + message.getTopic());
    }


    private void handleTopicExitMessage(TopicExitMessage message, ZMQ.Socket socket) throws IOException {

        if (message.getTopic() == null){
            throw new IOException("[Client Sub] closed connections");
        }

        this.isConnected = false;
        ChatServerEntry chatServerEntry = message.getChatServerEntry();
        socket.unsubscribe(message.getTopic());
        socket.disconnect(chatServerEntry.extPubAddress());

        System.out.println("[Client Sub] unsubcribe: " + message.getTopic());
        System.out.println("[Client Sub] disconnect: " + chatServerEntry.extPubAddress());
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

        System.out.println("[Client Sub] connect: " + inprocAddres);
        System.out.println("[Client Sub] subscribe: " + internalTopic);

        try{

            while (true) {

                Message message = carrier.receiveMessageWithTopic();

                if (message == null && this.isConnected) {
                    this.signals.put(new TimeoutSignal());
                    System.out.println("[Client Sub] timeout");
                }

                else if (message != null && !message.getType().equals(MessageType.HEART_BEAT)) {

                    System.out.println("[Client Sub] received: " + message);

                    switch (message){
                        case ChatMessage m -> handleChatMessage(m);
                        case TopicExitMessage m -> handleTopicExitMessage(m, socket);
                        case TopicEnterMessage m -> handleTopicEnterMessage(m, socket);
                        default -> System.out.println("[Client Sub] unknown: " + message);
                    }
                }
            }
        }

        catch (IOException e){
            socket.unsubscribe(internalTopic);
            socket.close();
            System.out.println(e.getMessage());
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}