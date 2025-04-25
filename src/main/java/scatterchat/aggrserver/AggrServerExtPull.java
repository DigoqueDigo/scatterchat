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
    private ZContext context;
    private BlockingQueue<Message> received;


    public AggrServerExtPull(JSONObject config, ZContext context, BlockingQueue<Message> received) {
        this.config = config;
        this.context = context;
        this.received = received;
    }


    @Override
    public void run() {

        try {

            ZMQ.Socket socket = this.context.createSocket(SocketType.PULL);
            ZMQCarrier carrier = new ZMQCarrier(socket);
            
            Message message;
            String bindAddres = config.getString("tcpExtPull");

            socket.bind(bindAddres);
            System.out.println("[AggrServerExtPull] started");
            System.out.println("[AggrServerExtPull] bind: " + bindAddres);

            while ((message = carrier.receiveMessage()) != null) {
                System.out.println("[AggrServerExtPull] received: " + message);
                this.received.put(message);
            }

            socket.close();
        }

        catch (Exception e) {
            e.printStackTrace();;
        }
    }
}