package scatterchat.chatserver;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.crtd.UserORSetMessage;
import scatterchat.protocol.message.info.ServeTopicRequest;

import java.util.concurrent.BlockingQueue;


public class ChatServerInterSub implements Runnable {

    private JSONObject config;
    private ZContext context;
    private BlockingQueue<CausalMessage> received;


    public ChatServerInterSub(JSONObject config, ZContext context, BlockingQueue<CausalMessage> received) {
        this.config = config;
        this.context = context;
        this.received = received;
    }


    private void forwardCausalMessage(CausalMessage message) throws InterruptedException {
        this.received.put(message);
    }


    private void handleServeTopicRequest(ServeTopicRequest message, ZMQ.Socket socket) {

        for (ChatServerEntry entry : message.getNodes()) {
            socket.connect(entry.interPubAddress());
            System.out.println("[SC interSub] connect: " + entry.interPubAddress());
        }

        socket.subscribe(message.getTopic());
        System.out.println("[SC interSub] subscribe: " + message.getTopic());
    }


    @Override
    public void run() {

        try {

            CausalMessage causalMessage;
            ZMQ.Socket socket = this.context.createSocket(SocketType.SUB);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            String tcpAddress = config.getString("tcpInterPub");
            String internalTopic = config.getString("internalTopic");

            socket.connect(tcpAddress);
            socket.subscribe(internalTopic);

            System.out.println("[SC interSub] started");
            System.out.println("[SC interSub] connect: " + tcpAddress);
            System.out.println("[SC interSub] subscribe: " + internalTopic);

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
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}