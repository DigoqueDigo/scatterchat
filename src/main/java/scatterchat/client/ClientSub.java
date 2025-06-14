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

    private String topic;
    private ZContext context;
    private JSONObject config;
    private BlockingQueue<Signal> signals;


    public ClientSub(JSONObject config, ZContext context, BlockingQueue<Signal> signals) {
        this.config = config;
        this.context = context;
        this.signals = signals;
    }


    private void handleChatMessage(ChatMessage message) {
        String content = String.format("%s > %s", message.getClient(), message.getMessage());
        ClientUI.appendToInBox(content);
    }


    private void handleTopicEnterMessage(TopicEnterMessage message, ZMQ.Socket socket) {
        this.topic = message.getTopic();
        ChatServerEntry chatServerEntry = message.getChatServerEntry();

        socket.connect(chatServerEntry.extPubAddress());
        socket.subscribe(message.getTopic());

        ClientUI.clearInfo();
        ClientUI.clearInBox();

        ClientUI.appendToLogs("[Client Sub] connect: " + chatServerEntry.extPubAddress());
        ClientUI.appendToLogs("[Client Sub] subcribe: " + message.getTopic());
    }


    private void handleTopicExitMessage(TopicExitMessage message, ZMQ.Socket socket) throws IOException {

        if (message.getTopic() == null){
            throw new IOException("[Client Sub] closed connections");
        }

        this.topic = null;
        ChatServerEntry chatServerEntry = message.getChatServerEntry();

        socket.unsubscribe(message.getTopic());
        socket.disconnect(chatServerEntry.extPubAddress());

        ClientUI.appendToLogs("[Client Sub] unsubcribe: " + message.getTopic());
        ClientUI.appendToLogs("[Client Sub] disconnect: " + chatServerEntry.extPubAddress());
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

        ClientUI.appendToLogs("[Client Sub] connect: " + inprocAddres);
        ClientUI.appendToLogs("[Client Sub] subscribe: " + internalTopic);

        try{

            while (true) {

                Message message = carrier.receiveMessageWithTopic();

                if (message == null && this.topic != null) {
                    this.signals.put(new TimeoutSignal(this.topic));
                    ClientUI.appendToLogs("[Client Sub] timeout");
                }

                else if (message != null && !message.getType().equals(MessageType.HEART_BEAT)) {

                    ClientUI.appendToLogs("[Client Sub] received: " + message);

                    switch (message){
                        case ChatMessage m -> handleChatMessage(m);
                        case TopicExitMessage m -> handleTopicExitMessage(m, socket);
                        case TopicEnterMessage m -> handleTopicEnterMessage(m, socket);
                        default -> ClientUI.appendToLogs("[Client Sub] unknown: " + message);
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