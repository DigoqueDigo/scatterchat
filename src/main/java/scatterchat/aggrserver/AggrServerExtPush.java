package scatterchat.aggrserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;


public class AggrServerExtPush implements Runnable {

    private ZContext context;
    private BlockingQueue<Message> outBuffer;
    private Map<String, ZMQCarrier> carriers;


    public AggrServerExtPush(ZContext contex, BlockingQueue<Message> outBuffer) {
        this.context = contex;
        this.outBuffer = outBuffer;
        this.carriers = new HashMap<>();
    }


    @Override
    public void run() {

        try {

            while (true) {

                Message message = this.outBuffer.take();
                String connection = message.getReceiver();
                System.out.println("[AggrServerExtPush] received: " + message);

                if (!this.carriers.containsKey(connection)) {
                    ZMQ.Socket socket = this.context.createSocket(SocketType.PUSH);
                    socket.connect(connection);
                    this.carriers.put(connection, new ZMQCarrier(socket));
                    System.out.println("[AggrServerExtPush] connect: " + message.getReceiver());
                }

                ZMQCarrier carrier = this.carriers.get(message.getReceiver());
                carrier.sendMessage(message);
                System.out.println("[AggrServerExtPush] sent: " + message);
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}