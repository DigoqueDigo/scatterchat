package scatterchat.aggrserver;

import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;


public class AggrServerExtPull implements Runnable {

    private JSONObject config;
    private BlockingQueue<Message> received;


    public AggrServerExtPull(JSONObject config, BlockingQueue<Message> received) {
        this.config = config;
        this.received = received;
    }


    @Override
    public void run() {

        try {

            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.PULL);

            ZMQCarrier carrier = new ZMQCarrier(socket);
            String bindAddres = config.getString("tcpExtPull");

            socket.bind(bindAddres);
            System.out.println("[AggrServerExtPull] started");
            System.out.println("[AggrServerExtPull] bind: " + bindAddres);

            Message message = null;

            while ((message = carrier.receiveMessage()) != null) {
                System.out.println("[AggrServerExtPull] received: " + message);
                this.received.put(message);
            }

            socket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();;
        }
    }
}