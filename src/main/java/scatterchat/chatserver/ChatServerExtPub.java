package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MessageType;


public class ChatServerExtPub implements Runnable{

    private String extPubAddress;
    private BlockingQueue<Message> delivered;


    public ChatServerExtPub(String extPubAddress, BlockingQueue<Message> delivered){
        this.extPubAddress = extPubAddress;
        this.delivered = delivered;
    }


    @Override
    public void run(){

        try{
            ZContext context = new ZContext();
            ZMQ.Socket pubSocket = context.createSocket(SocketType.PUB);
            pubSocket.bind(this.extPubAddress);

            Message message = null;
            Carrier pubCarrier = new Carrier(pubSocket);

            System.out.println("[SC extPub] started on: " + this.extPubAddress);

            while ((message = this.delivered.take()) != null){

                System.out.println("[SC extPub] Received: " + message);

                if (message.getType() == MessageType.CHAT_MESSAGE){
                    pubCarrier.sendWithTopic(message);
                }

                else if (message.getType() == MessageType.LOGGED_USERS_REQUEST){
                    // TODO :: SACAR OS UTILIZADORES QUE ESTAO LOGIN E ENVIAR
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