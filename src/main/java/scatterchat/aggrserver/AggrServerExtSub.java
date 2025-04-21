package scatterchat.aggrserver;

import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.cyclon.CyclonEntry;


public class AggrServerExtSub implements Runnable {

    private JSONObject config;
    private CyclonEntry entryPoint;
    private BlockingQueue<Message> received;


    public AggrServerExtSub(JSONObject config, CyclonEntry entryPoint, BlockingQueue<Message> received) {
        this.config = config;
        this.entryPoint = entryPoint;
        this.received = received;
    }


    @Override
    public void run() {
        try {
            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            socket.connect(config.getString("inprocPubSub"));
            socket.subscribe(config.getString("identity"));

            System.out.println("[AggrServerSub] Connect: " + config.getString("inprocPubSub"));
            System.out.println("[AggrServerSub] Subscribe: " + config.getString("identity"));

            if (this.entryPoint != null) {
                socket.connect(this.entryPoint.pubAddress());
                socket.connect(this.entryPoint.pubTimerAddress());
                System.out.println("[AggrServerSub] Connect: " + this.entryPoint.pubAddress());
                System.out.println("[AggrServerSub] Connect: " + this.entryPoint.pubTimerAddress());
            }

            Message message = null;

            while ((message = carrier.receiveMessageWithTopic()) != null) {
                received.put(message);
                // TODO :: FALTA TRATAR O PEDIDO INTERNO PARA ATUALIZAR OS VIZINHOS
            }

            socket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();;
        }
    }
}