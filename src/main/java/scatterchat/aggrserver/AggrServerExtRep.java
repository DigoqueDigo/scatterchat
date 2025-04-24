package scatterchat.aggrserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.JSONCarrier;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.aggr.AggrRep;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.dht.DHTPut;
import scatterchat.protocol.message.dht.IJson;
import scatterchat.protocol.message.info.ServeTopicRequest;
import scatterchat.protocol.message.info.ServeTopicResponse;


public class AggrServerExtRep implements Runnable {

    private JSONObject config;
    private BlockingQueue<Message> received;
    private BlockingQueue<AggrRep> responded;


    public AggrServerExtRep(JSONObject config, BlockingQueue<Message> received, BlockingQueue<AggrRep> responded) {
        this.config = config;
        this.received = received;
        this.responded = responded;
    }

    private void registerTopic(AggrRep message, JSONCarrier carrier) throws IOException{

        System.out.println("AQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQ");

        String topic = message.getTopic();
        List<String> ips = message.getEntries()
            .stream()
            .map(x -> x.chatServerEntry().repAddress())
            .toList();

        IJson dhtPut = new DHTPut(1, topic, ips);
        carrier.send(dhtPut);
        System.out.println("[AggrServerExtRep] sent: " + dhtPut);
    }

    private void informToServe(AggrRep message) {

        System.out.println("KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK");
        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);
        ZMQCarrier carrier = new ZMQCarrier(socket);

        String topic = message.getTopic();
        Set<ChatServerEntry> selectedNodes = message.getEntries()
            .stream()
            .map(x -> x.chatServerEntry())
            .collect(Collectors.toSet());

        for (ChatServerEntry selectedNode : selectedNodes) {

            socket.connect(selectedNode.repAddress());

            ServeTopicRequest serveTopicRequest = new ServeTopicRequest("sa", selectedNode.repAddress(), topic, selectedNodes);
            System.out.println("[AggrServerExtRep] sent: " + serveTopicRequest);
            System.out.println("ANTES DE ENVIAR");
            carrier.sendMessage(serveTopicRequest);

            System.out.println("[AggrServerExtRep] sent: " + serveTopicRequest);

            ServeTopicResponse serveTopicResponse = (ServeTopicResponse) carrier.receiveMessage();
            System.out.println("[AggrServerExtRep] received: " + serveTopicResponse);

            socket.disconnect(selectedNode.repAddress());
        }

        socket.close();
        context.close();
    }


    @Override
    public void run() {

        try {

            Socket dhtSocket = new Socket();
            ZContext context = new ZContext();
            ZMQ.Socket repSocket = context.createSocket(SocketType.REP);
            
            String bindAddress = config.getString("tcpExtRep");
            String dhtAddress = config.getJSONObject("dht").getString("address");
            int dhtPort = config.getJSONObject("dht").getInt("port");
            
            repSocket.bind(bindAddress);
            dhtSocket.connect(new InetSocketAddress(dhtAddress, dhtPort));

            ZMQCarrier repCarrier = new ZMQCarrier(repSocket);
            JSONCarrier dhtCarrier = new JSONCarrier(dhtSocket);

            System.out.println("[AggrServerExtRep] started");
            System.out.println("[AggrServerExtRep] bind: " + bindAddress);
            System.out.println("[AggrServerExtRep] connect: " + dhtAddress + ":" + dhtPort);

            Message message;
            AggrRep response;

            while ((message = repCarrier.receiveMessage()) != null) {

                received.put(message);
                System.out.println("[AggrServerExtRep] received: " + message);

                response = responded.take();

                if (response.getSuccess()) {
                    registerTopic(response, dhtCarrier);
                    informToServe(response);
                }

                repCarrier.sendMessage(response);
                System.out.println("[AggrServerExtRep] sent: " + response);
            }

            repSocket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();;
        }
    }
}