package scatterchat.client;

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


public class ClientSub implements Runnable{

    private JSONObject config;


    public ClientSub(JSONObject config){
        this.config = config;
    }


    private void handleChatMessage(ChatMessage message){
        System.out.println(message.getMessage());
    }


    private void handleTopicEnterMessage(TopicEnterMessage message, ZMQ.Socket socket){
        ChatServerEntry chatServerEntry = message.getChatServerEntry();
        socket.connect(chatServerEntry.extPubAddress());
        socket.subscribe(message.getTopic());
    }


    private void handleTopicExitMessage(TopicExitMessage message, ZMQ.Socket socket) throws NullPointerException{

        if (message.getTopic() == null){
            throw new NullPointerException("client exit");
        }

        ChatServerEntry chatServerEntry = message.getChatServerEntry();
        socket.unsubscribe(message.getTopic());
        socket.disconnect(chatServerEntry.extPubAddress());
    }


    @Override
    public void run() {

        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.SUB);
        ZMQCarrier carrier = new ZMQCarrier(socket);

        String inprocAddres = config.getString("inprocPubSub");
        String internalTopic = config.getString("internalTopic");

        socket.connect(inprocAddres);
        socket.subscribe(internalTopic);

        System.out.println("[Client SUB] connected: " + inprocAddres);
        System.out.println("[Client SUB] subscribe: " + internalTopic);

        try{

            Message message = null;

            while ((message = carrier.receiveMessageWithTopic()) != null){

                System.out.println("[Client SUB] received: " + message);

                switch (message){
                    case ChatMessage m -> handleChatMessage(m);
                    case TopicExitMessage m -> handleTopicExitMessage(m, socket);
                    case TopicEnterMessage m -> handleTopicEnterMessage(m, socket);
                    default -> System.out.println("[Client SUB] unknown: " + message);
                }
            }
        }

        catch (NullPointerException e){
            socket.unsubscribe("[internal]");
            socket.close();
            context.close();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}