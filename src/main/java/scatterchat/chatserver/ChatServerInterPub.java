package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.Message;


public class ChatServerInterPub implements Runnable{

    private State state;
    private String interPubAddress;
    private BlockingQueue<Message> broadcast;


    public ChatServerInterPub(String interPubAddress, State state, BlockingQueue<Message> broadcast){
        this.interPubAddress = interPubAddress;
        this.state = state;
        this.broadcast = broadcast;
    }


    @Override
    public void run(){

        try{
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PUB);
            socket.bind(this.interPubAddress);

            Message message = null;
            Carrier carrier = new Carrier(socket);

            // TODO :: UTILIZAR O STATE PARA CONECTAR-ME AOS SC DO CHAT

            while ((message = broadcast.take()) != null){
                carrier.sendWithTopic(message);
            }

            socket.close();
            context.close();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }   
}