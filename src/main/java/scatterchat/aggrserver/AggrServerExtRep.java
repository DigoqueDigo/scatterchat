package scatterchat.aggrserver;

import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.aggr.AggrRep;


public class AggrServerExtRep implements Runnable {

    private JSONObject config;
    private BlockingQueue<Message> received;
    private BlockingQueue<AggrRep> responded;


    public AggrServerExtRep(JSONObject config, BlockingQueue<Message> received, BlockingQueue<AggrRep> responded) {
        this.config = config;
        this.received = received;
        this.responded = responded;
    }


    @Override
    public void run() {

        try {

            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.REP);

            ZMQCarrier carrier = new ZMQCarrier(socket);
            String bindAddress = config.getString("tcpExtRep");

            socket.bind(bindAddress);
            System.out.println("[AggrServerExtRep] started");
            System.out.println("[AggrServerExtRep] bind: " + bindAddress);

            Message message = null;

            while ((message = carrier.receiveMessage()) != null) {

                received.put(message);
                System.out.println("[AggrServerExtRep] received: " + message);

                message = responded.take();
                carrier.sendMessage(message);
                System.out.println("[AggrServerExtRep] sent: " + message);
            }

            socket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();;
        }
    }
}