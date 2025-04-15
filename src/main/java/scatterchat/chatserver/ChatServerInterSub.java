package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.ChatMessage;
import scatterchat.protocol.messages.GroupJoinMessage;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MESSAGE_TYPE;


public class ChatServerInterSub implements Runnable{

    private String nodeId;
    private String interPubAddress;
    private BlockingQueue<Message> delivered;


    public ChatServerInterSub(String nodeId, String interPubAddress, BlockingQueue<Message> delivered){
        this.nodeId = nodeId;
        this.interPubAddress = interPubAddress;
        this.delivered = delivered;
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
            carrier.on(MESSAGE_TYPE.GROUP_JOIN_WARNING, x -> GroupJoinMessage.deserialize(x));

            while ((message = carrier.receiveWithTopic()) != null){

                if (message.getType() == MESSAGE_TYPE.CHAT_MESSAGE){
                    // TODO :: TENHO DE GARANTIR A ORDEM CAUSAL
                    this.delivered.put(message);
                }

                else if (message.getType() == MESSAGE_TYPE.GROUP_JOIN_WARNING){

                    String topic = message.getTopic().replace("[internal]", "");
                    GroupJoinMessage groupJoinWarning = (GroupJoinMessage) message;
                    subSocket.subscribe(topic);

                    for (String nodeInterPubAddres : groupJoinWarning.getNodes()){
                        if (!nodeInterPubAddres.equals(nodeId)){
                            subSocket.connect(nodeInterPubAddres);
                        }
                    }
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