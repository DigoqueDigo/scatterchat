package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MESSAGE_TYPE;
import scatterchat.protocol.messages.chat.ChatExitMessage;
import scatterchat.protocol.messages.chat.ChatMessage;
import scatterchat.protocol.messages.crtd.OrSetMessage;
import scatterchat.protocol.messages.crtd.OrSetMessage.OPERATION;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.Carrier;


public class ChatServerExtPull implements Runnable{

    private State state;
    private String extPullAddress; 
    private BlockingQueue<Message> broadcast;


    public ChatServerExtPull(String extPullAddress, State state, BlockingQueue<Message> broadcast){
        this.extPullAddress = extPullAddress;
        this.state = state;
        this.broadcast = broadcast;
    }


    private void handleChatMessage(ChatMessage message) throws InterruptedException{

        // TODO :: VERIFICAR SE O CLIENT AINDA NÃO FOI REGISTADO NO TOPICO
        this.broadcast.put(message);
    }


    private void handleChatExitMessage(ChatExitMessage message) throws InterruptedException{

        synchronized (state){

            OrSetMessage orSetMessage = new OrSetMessage(
                message.getTopic(),
                message.getSender(),
                state.getClockOf(message.getTopic()),
                OPERATION.REMOVE,
                state.getCRDTClockUsers(),
                state.);
            this.broadcast.put(message);

        }

        // TODOO :: RETIRAR O CLIENTE DO TOPICO

    }


    @Override
    public void run(){

        try{
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PULL);
            socket.bind(this.extPullAddress);

            Message message = null;
            Carrier carrier = new Carrier(socket);
            System.out.println("[SC extPull] started on: " + this.extPullAddress);

            carrier.on(MESSAGE_TYPE.CHAT_MESSAGE, x -> ChatMessage.deserialize(x));
            carrier.on(MESSAGE_TYPE.CHAT_EXIT_MESSAGE, x -> ChatExitMessage.deserialize(x));

            while ((message = carrier.receive()) != null){

                System.out.println("[SC extPull] Received: " + message.toString());

                switch (message){
                    case ChatMessage m -> handleChatMessage(m);
                    case ChatExitMessage m -> handleChatExitMessage(m);
                    default -> System.out.println("[SC extPull] Unknown: " + message);
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