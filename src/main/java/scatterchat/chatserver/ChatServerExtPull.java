package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.messages.ChatMessage;
import scatterchat.protocol.messages.GroupJoinMessage;
import scatterchat.protocol.messages.LoggedUsersMessage;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MESSAGE_TYPE;
import scatterchat.protocol.carrier.Carrier;


public class ChatServerExtPull implements Runnable{

    private String extPullAddress; 
    private BlockingQueue<Message> delivered;
    private BlockingQueue<Message> broadcast;


    public ChatServerExtPull(String extPullAddress, BlockingQueue<Message> delivered, BlockingQueue<Message> broadcast){
        this.extPullAddress = extPullAddress;
        this.delivered = delivered;
        this.broadcast = broadcast;
    }


    @Override
    public void run(){

        try{
            ZContext context = new ZContext();
            ZMQ.Socket pullSocket = context.createSocket(SocketType.PULL);
            pullSocket.bind(this.extPullAddress);

            Message message = null;
            Carrier pullCarrier = new Carrier(pullSocket);

            pullCarrier.on(MESSAGE_TYPE.CHAT_MESSAGE, x -> ChatMessage.deserialize(x));
            pullCarrier.on(MESSAGE_TYPE.GROUP_JOIN_WARNING, x -> GroupJoinMessage.deserialize(x));
            pullCarrier.on(MESSAGE_TYPE.LOGGED_USERS_REQUEST, x -> LoggedUsersMessage.deserialize(x));

            System.out.println("[SC extPull] started on: " + this.extPullAddress);

            while ((message = pullCarrier.receive()) != null){

                System.out.println("[SC extPull] Received: " + message.toString());

                if (message.getType() == MESSAGE_TYPE.CHAT_MESSAGE){
                    this.broadcast.put(message);
                }

                else if (message.getType() == MESSAGE_TYPE.GROUP_JOIN_WARNING){
                    this.broadcast.put(message);
                    this.delivered.put(message);
                }

                else if (message.getType() == MESSAGE_TYPE.LOGGED_USERS_REQUEST){
                    this.delivered.put(message);
                }
            }

            pullSocket.close();
            context.close();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}