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

    private BlockingQueue<Message> outBuffer;
    private Map<String, ZMQCarrier> carriers;


    public AggrServerExtPush(BlockingQueue<Message> outBuffer) {
        this.outBuffer = outBuffer;
        this.carriers = new HashMap<>();
    }


    @Override
    public void run() {

        try {
            Message message = null;
            ZContext context = new ZContext();

            while ((message = this.outBuffer.take()) != null) {

                if (!this.carriers.containsKey(message.getReceiver())) {
                    ZMQ.Socket socket = context.createSocket(SocketType.PUSH);
                    socket.connect(message.getReceiver());
                    this.carriers.put(message.getReceiver(), new ZMQCarrier(socket));
                }

                ZMQCarrier carrier = this.carriers.get(message.getReceiver());
                carrier.sendMessage(message);
            }

            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}