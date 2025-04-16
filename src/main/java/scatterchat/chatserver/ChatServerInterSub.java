package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.CausalMessage;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MESSAGE_TYPE;
import scatterchat.protocol.messages.chat.ChatExitMessage;
import scatterchat.protocol.messages.chat.ChatMessage;
import scatterchat.protocol.messages.info.ServeTopicRequest;


public class ChatServerInterSub implements Runnable{

    private String nodeId;
    private String interPubAddress;
    private BlockingQueue<CausalMessage> received;


    public ChatServerInterSub(String nodeId, String interPubAddress, BlockingQueue<CausalMessage> received){
        this.nodeId = nodeId;
        this.interPubAddress = interPubAddress;
        this.received = received;
    }


    private void handleChatMessage(ChatMessage message) throws InterruptedException{
        this.received.put(message);
    }


    public void handleChatExitMessage(ChatExitMessage message) throws InterruptedException{
        this.received.put(message);
    }


    private void handleServeTopicRequest(ServeTopicRequest message, ZMQ.Socket subSocket){

        String topic = message.getTopic().replace("[internal]", "");
        ServeTopicRequest serveTopicRequest = (ServeTopicRequest) message;
        subSocket.subscribe(topic);

        for (String nodeInterPubAddres : serveTopicRequest.getNodes()){
            if (!nodeInterPubAddres.equals(nodeId)){
                subSocket.connect(nodeInterPubAddres);
            }
        }
    }


    @Override
    public void run(){

        try{
            ZContext context = new ZContext();
            ZMQ.Socket subSocket = context.createSocket(SocketType.SUB);

            subSocket.connect(interPubAddress);
            subSocket.subscribe("[internal]");
            System.out.println("[SC interSub] started");

            Message message = null;
            Carrier carrier = new Carrier(subSocket);

            carrier.on(MESSAGE_TYPE.CHAT_MESSAGE, x -> ChatMessage.deserialize(x));
            carrier.on(MESSAGE_TYPE.CHAT_EXIT_MESSAGE, x -> ChatExitMessage.deserialize(x));
            carrier.on(MESSAGE_TYPE.SERVE_TOPIC_REQUEST, x -> ServeTopicRequest.deserialize(x));

            while ((message = carrier.receiveWithTopic()) != null){

                System.out.println("[SC interSub] Received: " + message);

                switch (message){
                    case ChatMessage m -> handleChatMessage(m);
                    case ChatExitMessage m -> handleChatExitMessage(m);
                    case ServeTopicRequest m -> handleServeTopicRequest(m, subSocket);
                    default -> System.out.println("[SC interSub] Unknown: " + message);
                }
            }

            subSocket.close();
            context.close();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}