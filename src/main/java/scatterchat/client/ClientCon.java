package scatterchat.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.JSONCarrier;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.signal.ChatMessageSignal;
import scatterchat.protocol.signal.ExitSginal;
import scatterchat.protocol.signal.LogRequestSignal;
import scatterchat.protocol.signal.ServeStateRequestSignal;
import scatterchat.protocol.signal.Signal;
import scatterchat.protocol.signal.TimeoutSignal;
import scatterchat.protocol.signal.TopicEnterSignal;
import scatterchat.protocol.signal.TopicExitSignal;
import scatterchat.protocol.signal.UserMessagesRequestSignal;


public class ClientCon implements Runnable {

    private ZContext context;
    private JSONObject config;
    private BlockingQueue<Signal> signals;

    private int currentTopic;
    private ChatServerEntry currentChatServerEntry;

    private Socket dhtSocket;
    private ZMQ.Socket pushSCSocket;
    private ZMQ.Socket reqSCSocket;
    private ZMQ.Socket reqSASocket;
    private ZMQ.Socket pubSocket;

    private JSONCarrier dhtCarrier;
    private ZMQCarrier pushSCCarrier;
    private ZMQCarrier reqSCCarrier;
    private ZMQCarrier reqSACarrier;
    private ZMQCarrier pubCarrier;


    public ClientCon(JSONObject config, ZContext context, BlockingQueue<Signal> signals) {
        this.config = config;
        this.context = context;
        this.signals = signals;
    }
    
    
    public void setupConnections() throws IOException {
        this.pushSCSocket = this.context.createSocket(SocketType.PUSH);
        this.reqSCSocket = this.context.createSocket(SocketType.REQ);
        this.reqSASocket = this.context.createSocket(SocketType.REQ);
        this.pubSocket = this.context.createSocket(SocketType.PUB);

        int dhtPort = config.getJSONObject("dht").getInt("port");
        String dhtAddress = config.getJSONObject("dht").getString("address");
        String repSAAddress = config.getJSONObject("sa").getString("tcpExtRep");
        String inprocAddress = config.getString("inprocPubSub");
    
        pubSocket.bind(inprocAddress);
        reqSASocket.connect(repSAAddress);
        dhtSocket.connect(new InetSocketAddress(dhtAddress, dhtPort));        

        this.pushSCCarrier = new ZMQCarrier(pushSCSocket);
        this.reqSCCarrier = new ZMQCarrier(reqSCSocket);
        this.reqSACarrier = new ZMQCarrier(reqSASocket);
        this.pubCarrier = new ZMQCarrier(pubSocket);
        this.dhtCarrier = new JSONCarrier(dhtSocket);

        System.out.println("[ClientCon] bind: " + inprocAddress);
        System.out.println("[ClientCon] connect: " + repSAAddress);
        System.out.println("[ClientCon] connnect: " + dhtAddress + ":" + dhtPort);
    }


    public void closeConnections() throws IOException {
        this.dhtSocket.close();
        this.pubSocket.close();
        this.reqSCSocket.close();
        this.reqSASocket.close();
        this.pushSCSocket.close();
    }

    
    private Object handleUserMessagesRequestSignal(UserMessagesRequestSignal sig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleUserMessagesRequestSignal'");
    }


    private Object handleTopicExitSignal(TopicExitSignal sig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleTopicExitSignal'");
    }


    private Object handleTopicEnterSignal(TopicEnterSignal sig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleTopicEnterSignal'");
    }


    private Object handleTimeoutSignal(TimeoutSignal sig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleTimeoutSignal'");
    }


    private Object handleServeStateSignal(ServeStateRequestSignal sig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleServeStateSignal'");
    }


    private Object handleLogRequestSignal(LogRequestSignal sig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleLogRequestSignal'");
    }


    private Object handleExitSignal(ExitSginal sig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleExitSignal'");
    }


    private Object handleChatMessageSignla(ChatMessageSignal sig) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'handleChatMessageSignla'");
    }


    @Override
    public void run() {

        try {
            Signal signal;
            this.setupConnections();
            Map<Class<?>, Consumer<Signal>> handlers = new HashMap<>();

            handlers.put(ChatMessageSignal.class, sig -> handleChatMessageSignla((ChatMessageSignal) sig));
            handlers.put(ExitSginal.class, sig -> handleExitSignal((ExitSginal) sig));
            handlers.put(LogRequestSignal.class, sig -> handleLogRequestSignal((LogRequestSignal) sig));
            handlers.put(ServeStateRequestSignal.class, sig -> handleServeStateSignal((ServeStateRequestSignal) sig));
            handlers.put(TimeoutSignal.class, sig -> handleTimeoutSignal((TimeoutSignal) sig));
            handlers.put(TopicEnterSignal.class, sig -> handleTopicEnterSignal((TopicEnterSignal) sig));
            handlers.put(TopicExitSignal.class, sig -> handleTopicExitSignal((TopicExitSignal) sig));
            handlers.put(UserMessagesRequestSignal.class, sig -> handleUserMessagesRequestSignal((UserMessagesRequestSignal) sig));

            while ((signal = this.signals.take()) != null) {
                if (handlers.containsKey(signal.getClass())) {
                    handlers.get(signal.getClass()).accept(signal);
                } else {
                    System.out.println("[ClientCon] unknown: " + signal);
                }
            }

            this.closeConnections();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}