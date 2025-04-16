package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.deliver.Deliver;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MESSAGE_TYPE;
import scatterchat.protocol.messages.chat.ChatMessage;
import scatterchat.protocol.messages.info.ServeTopicRequest;


public class ChatServerInterSub implements Runnable{

    private String nodeId;
    private String interPubAddress;
    private Deliver deliver;


    public ChatServerInterSub(String nodeId, String interPubAddress, BlockingQueue<Message> delivered){
        this.nodeId = nodeId;
        this.interPubAddress = interPubAddress;
        this.deliver = new Deliver(delivered);
    }


    private void handleChatMessage(ChatMessage message){
        String topic = message.getTopic();
        this.deliver.addPendingMessage(topic, message);
    }


    private void handleGroupJoinMessage(ServeTopicRequest message, ZMQ.Socket subSocket){

        String topic = message.getTopic().replace("[internal]", "");
        ServeTopicRequest groupJoinWarning = (ServeTopicRequest) message;
        subSocket.subscribe(topic);

        for (String nodeInterPubAddres : groupJoinWarning.getNodes()){
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

            Message message = null;
            Carrier carrier = new Carrier(subSocket);

            carrier.on(MESSAGE_TYPE.CHAT_MESSAGE, x -> ChatMessage.deserialize(x));
            carrier.on(MESSAGE_TYPE.GROUP_JOIN_WARNING, x -> ServeTopicRequest.deserialize(x));

            System.out.println("[SC interSub] started");

            while ((message = carrier.receiveWithTopic()) != null){

                System.out.println("[SC interSub] Received: " + message);

                switch (message){
                    case ChatMessage m -> handleChatMessage(m);
                    case ServeTopicRequest m -> handleGroupJoinMessage(m, subSocket);
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