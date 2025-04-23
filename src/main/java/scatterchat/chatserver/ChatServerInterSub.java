package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;
import scatterchat.protocol.message.info.ServeTopicRequest;

import java.util.concurrent.BlockingQueue;


public class ChatServerInterSub implements Runnable {

    private JSONObject config;
    private BlockingQueue<CausalMessage> received;


    public ChatServerInterSub(JSONObject config, BlockingQueue<CausalMessage> received) {
        this.config = config;
        this.received = received;
    }


    private void forwardCausalMessage(CausalMessage message) throws InterruptedException {
        this.received.put(message);
    }


    private void handleServeTopicRequest(ServeTopicRequest message, ZMQ.Socket socket) {
        message.getNodes().forEach(entry -> socket.connect(entry.interPubAddress()));
        socket.subscribe(message.getTopic());
    }


    @Override
    public void run() {

        try {

            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.SUB);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            String internalTopic = config.getString("internalTopic");
            String inprocAddress = config.getString("inprocPubSub");  

            socket.connect(inprocAddress);
            socket.subscribe(internalTopic);

            System.out.println("[SC interSub] started");
            System.out.println("[SC interSub] connect: " + inprocAddress);
            System.out.println("[SC interSub] subscribe: [internal]" + internalTopic);

            CausalMessage causalMessage = null;

            while ((causalMessage = carrier.receiveCausalMessageWithTopic()) != null) {

                System.out.println("[SC interSub] received: " + causalMessage);

                switch (causalMessage.getMessage()) {
                    case ChatMessage m -> forwardCausalMessage(causalMessage);
                    case UserORSetMessage m -> forwardCausalMessage(causalMessage);
                    case ServeTopicRequest m -> handleServeTopicRequest(m, socket);
                    default -> System.out.println("[SC interSub] unknown: " + causalMessage);
                }
            }

            socket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}