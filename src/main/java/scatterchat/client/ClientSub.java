package scatterchat.client;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;


public class ClientSub implements Runnable{

    private JSONObject config;


    public ClientSub(JSONObject config){
        this.config = config;
    }


    private void handleChatMessage(ChatMessage message){
        System.out.println(message.getMessage());
    }


    private void handleTopicEnterMessage(TopicEnterMessage message, ZMQ.Socket socket){
        socket.subscribe(message.getTopic());
    }


    private void handleTopicExitMessage(TopicExitMessage message, ZMQ.Socket socket){
        socket.unsubscribe(message.getTopic());
    }


    @Override
    public void run() {

        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.SUB);

        Message message = null;
        Carrier carrier = new Carrier(socket);
        String pubAddress = config.getString("interPubProcAddress");

        socket.connect(pubAddress);
        socket.subscribe("[internal]");
        System.out.println("[Client SUB] started");

        while ((message = carrier.receiveMessageWithTopic()) != null){

            System.out.println("[Client SUB] received " + message);

            switch (message){
                case ChatMessage m -> handleChatMessage(m);
                case TopicExitMessage m -> handleTopicExitMessage(m, socket);
                case TopicEnterMessage m -> handleTopicEnterMessage(m, socket);
                default -> System.out.println("[Client SUB] Unknown " );
            }
        }

        socket.close();
        context.close();
    }
}