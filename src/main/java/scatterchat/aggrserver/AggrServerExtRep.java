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
    private BlockingQueue<AggrRep> response;


    public AggrServerExtRep(JSONObject config, BlockingQueue<Message> received, BlockingQueue<AggrRep> response) {
        this.config = config;
        this.received = received;
        this.response = response;
    }


    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            socket.bind(config.getString("tcpExtRep"));
            System.out.println("[AggrServerExtRep] bind: " + config.getString("tcpExtRep"));

            Message message = null;

            while ((message = carrier.receiveMessage()) != null) {
                received.put(message);
                carrier.sendMessage(response.take());
            }

            socket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();;
        }
    }
}