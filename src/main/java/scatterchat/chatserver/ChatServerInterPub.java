package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.chat.ChatExitMessage;
import scatterchat.protocol.messages.chat.ChatMessage;
import scatterchat.protocol.messages.info.ServeTopicRequest;


public class ChatServerInterPub implements Runnable{

    private String interPubAddress;
    private BlockingQueue<Message> broadcast;


    public ChatServerInterPub(String interPubAddress, BlockingQueue<Message> broadcast){
        this.interPubAddress = interPubAddress;
        this.broadcast = broadcast;
    }


    private void handleChatMessage(ChatMessage message, Carrier carrier){
        carrier.sendWithTopic(message);
    }


    private void handleChatExitMessage(ChatExitMessage message, Carrier carrier){
        message.setTopic("[internal]" + message.getTopic());
        carrier.sendWithTopic(message);
    }


    private void handleServeTopicRequest(ServeTopicRequest message, Carrier carrier){
        message.setTopic("[internal]" + message.getTopic());
        carrier.sendWithTopic(message);
    }


    @Override
    public void run(){

        try{
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PUB);
            socket.bind(this.interPubAddress);

            Message message = null;
            Carrier carrier = new Carrier(socket);
            System.out.println("[SC interPub] started on: " + this.interPubAddress);

            while ((message = broadcast.take()) != null){

                System.out.println("[SC interPub] Reveived: " + message.toString());

                switch (message){
                    case ChatMessage m -> handleChatMessage(m, carrier);
                    case ChatExitMessage m -> handleChatExitMessage(m, carrier);
                    case ServeTopicRequest m -> handleServeTopicRequest(m, carrier);
                    default -> System.out.println("[SC interPub] Unknown: " + message);
                }
            }

            socket.close();
            context.close();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}