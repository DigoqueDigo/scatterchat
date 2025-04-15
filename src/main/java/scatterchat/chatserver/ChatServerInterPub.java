package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MESSAGE_TYPE;


public class ChatServerInterPub implements Runnable{

    private String interPubAddress;
    private BlockingQueue<Message> broadcast;


    public ChatServerInterPub(String interPubAddress, BlockingQueue<Message> broadcast){
        this.interPubAddress = interPubAddress;
        this.broadcast = broadcast;
    }


    @Override
    public void run(){

        try{
            ZContext context = new ZContext();
            ZMQ.Socket pubSocket = context.createSocket(SocketType.PUB);
            pubSocket.bind(this.interPubAddress);

            Message message = null;
            Carrier pubCarrier = new Carrier(pubSocket);

            System.out.println("[SC interPuB] started on: " + this.interPubAddress);

            while ((message = broadcast.take()) != null){

                System.out.println("[SC interPuB] Reveived: " + message.toString());

                if (message.getType() == MESSAGE_TYPE.CHAT_MESSAGE){
                    pubCarrier.sendWithTopic(message);
                }

                else if (message.getType() == MESSAGE_TYPE.GROUP_JOIN_WARNING){
                    message.setTopic("[internal]" + message.getTopic());
                    pubCarrier.sendWithTopic(message);
                }
            }

            pubSocket.close();
            context.close();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}