package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
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

        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.PULL);
        socket.bind(this.extPullAddress);

        Message message = null;
        Carrier carrier = new Carrier(socket);

        while ((message = carrier.receive()) != null){

            if (message.getType() == MESSAGE_TYPE.CHAT_MESSAGE){
                this.broadcast.add(message);
            }

            else if (message.getType() == MESSAGE_TYPE.LOGGED_USERS_REQUEST){
                this.delivered.add(message);
            }
        }

        socket.close();
        context.close();
    }
}