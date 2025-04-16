package scatterchat.chatserver;
import java.util.concurrent.BlockingQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.protocol.messages.CausalMessage;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MESSAGE_TYPE;
import scatterchat.protocol.messages.chat.ChatExitMessage;
import scatterchat.protocol.messages.chat.ChatMessage;
import scatterchat.protocol.messages.crtd.ORSetMessage;
import scatterchat.protocol.messages.crtd.UsersORSetMessage;
import scatterchat.protocol.messages.crtd.ORSetMessage.OPERATION;
import scatterchat.chatserver.state.ORSet;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.Carrier;


public class ChatServerExtPull implements Runnable{

    private State state;
    private String extPullAddress; 
    private BlockingQueue<CausalMessage> broadcast;


    public ChatServerExtPull(String extPullAddress, State state, BlockingQueue<CausalMessage> broadcast){
        this.extPullAddress = extPullAddress;
        this.state = state;
        this.broadcast = broadcast;
    }


    private void handleChatMessage(ChatMessage message) throws InterruptedException{

        synchronized (state){

            final String topic = message.getTopic();
            final String sender = message.getSender();
            ORSet orSet = state.getUsersORSetOf(topic);

            if (!orSet.contains(sender)){

                ORSetMessage orSetMessage = orSet.prepare(OPERATION.ADD, sender);
                UsersORSetMessage usersORSetMessage = new UsersORSetMessage(topic, sender, orSetMessage);
                CausalMessage causalMessage = new CausalMessage(usersORSetMessage, state.getVectorClockOf(topic));

                orSet.effect(orSetMessage);
                broadcast.put(causalMessage);
            }

            CausalMessage causalMessage = new CausalMessage(message, state.getVectorClockOf(topic));
            broadcast.put(causalMessage);
        }
    }


    private void handleChatExitMessage(ChatExitMessage message) throws InterruptedException{

        synchronized (state){

            final String topic = message.getTopic();
            final String sender = message.getSender();

            ORSet orSet = state.getUsersORSetOf(topic);
            ORSetMessage orSetMessage = orSet.prepare(OPERATION.REMOVE, sender);

            UsersORSetMessage usersORSetMessage = new UsersORSetMessage(topic, sender, orSetMessage);
            CausalMessage causalMessage = new CausalMessage(usersORSetMessage, state.getVectorClockOf(topic));

            orSet.effect(orSetMessage);
            broadcast.put(causalMessage);
        }
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