package scatterchat.chatserver;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.carrier.Carrier;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MessageType;
import scatterchat.protocol.messages.info.ServerStateRequest;


public class ChatServerExtRep implements Runnable {

    private String extRepAddress;
    private State state;


    public ChatServerExtRep(String extRepAddress, State state) {
        this.extRepAddress = extRepAddress;
        this.state = state;
    }


    @Override
    public void run() {

        try {
            ZContext context = new ZContext();
            ZMQ.Socket repSocket = context.createSocket(SocketType.PUB);
            repSocket.bind(extRepAddress);

            Message message = null;
            Carrier repCarrier = new Carrier(repSocket);

            repCarrier.on(MessageType.SERVER_INFO, x -> ServerStateRequest.deserialize(x));
            repCarrier.on(MessageType.USERS_LOGGED, x -> UsersLoggedMessage.deserialize(x));

            System.out.println("[SC extRep] started on: " + extRepAddress);

            while ((message = repCarrier.receive()) != null) {

                System.out.println("[SC extRep] Received: " + message);

                switch (message) {
                    case ServerStateRequest m -> System.out.println(m);
                    case UsersLoggedMessage m -> System.out.println(m);
                    default -> System.out.println("[SC extRep] Unknown: " + message);
                }
            }

            repSocket.close();
            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}