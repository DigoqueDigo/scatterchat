package scatterchat.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.sarojaba.prettytable4j.PrettyTable;

import scatterchat.LogMessageRequest;
import scatterchat.UserMessagesRequest;
import scatterchat.protocol.carrier.JSONCarrier;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.aggr.AggrReq;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.message.dht.DHTGet;
import scatterchat.protocol.message.dht.DHTRep;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;
import scatterchat.protocol.signal.ChatMessageSignal;
import scatterchat.protocol.signal.EnterTopicSignal;
import scatterchat.protocol.signal.ExitSignal;
import scatterchat.protocol.signal.ExitTopicSignal;
import scatterchat.protocol.signal.LogSignal;
import scatterchat.protocol.signal.ServeStateSignal;
import scatterchat.protocol.signal.Signal;
import scatterchat.protocol.signal.TimeoutSignal;
import scatterchat.protocol.signal.UserMessagesSignal;


public class ClientCon implements Runnable {

    private ZContext context;
    private JSONObject config;
    private BlockingQueue<Signal> signals;
    
    private Random random;
    private String currentTopic;
    private ChatServerEntry chatServerEntry;
    private List<ChatServerEntry> chatServerEntries;

    private String sender;
    private int dhtPort;
    private String dhtAddress;
    private String repSAAddress;
    private String inprocAddress;
    private String internalTopic;

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

    private ClientLog clientLog;


    public ClientCon(JSONObject config, ZContext context, BlockingQueue<Signal> signals) {
        this.config = config;
        this.context = context;
        this.signals = signals;
        this.random = new Random();
        this.chatServerEntries = new ArrayList<>();
    }
    
    
    public void setupConnections() throws IOException {
        this.pushSCSocket = this.context.createSocket(SocketType.PUSH);
        this.reqSCSocket = this.context.createSocket(SocketType.REQ);
        this.reqSASocket = this.context.createSocket(SocketType.REQ);
        this.pubSocket = this.context.createSocket(SocketType.PUB);

        this.sender = config.getString("username");
        this.inprocAddress = config.getString("inprocPubSub");
        this.internalTopic = config.getString("internalTopic");

        this.dhtPort = config.getJSONObject("dht").getInt("port");
        this.dhtAddress = config.getJSONObject("dht").getString("address");
        this.repSAAddress = config.getJSONObject("sa").getString("tcpExtRep");

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


    private void handleTopicEnterSignal(EnterTopicSignal sig) throws IOException {

        DHTGet dhtGet = new DHTGet(0, sig.topic());
        this.dhtCarrier.send(dhtGet);
        DHTRep dhtRep = this.dhtCarrier.receive();

        while (dhtRep.ips().isEmpty()) {   
            AggrReq aggrReq = new AggrReq(this.sender, this.repSAAddress, sig.topic());
            this.reqSACarrier.sendMessage(aggrReq);
            this.reqSACarrier.receiveMessage();
            this.dhtCarrier.send(dhtGet);
            dhtRep = this.dhtCarrier.receive();
        }

        this.chatServerEntries = dhtRep.ips()
            .stream()
            .map(ip -> new ChatServerEntry(ip))
            .collect(Collectors.toList());

        int scIndex = this.random.nextInt(this.chatServerEntries.size());
        this.chatServerEntry = this.chatServerEntries.remove(scIndex);
        this.currentTopic = sig.topic();

        this.pushSCSocket.connect(this.chatServerEntry.pullAddress());
        this.reqSCSocket.connect(this.chatServerEntry.repAddress());

        this.clientLog = new ClientLog(
            this.chatServerEntry.loggerAddress(),
            this.chatServerEntry.loggerPort()
        );

        Message topicEnterMessageToSub =
            new TopicEnterMessage(this.sender, "clientsub", sig.topic(), this.chatServerEntry);

        Message topicEnterMessageToPull =
            new TopicEnterMessage(this.sender, this.chatServerEntry.pullAddress(), sig.topic(), this.chatServerEntry);  

        this.pushSCCarrier.sendMessage(topicEnterMessageToPull);
        this.pubCarrier.sendMessageWithTopic(this.internalTopic, topicEnterMessageToSub); 
    }


    private void handleTopicExitSignal(ExitTopicSignal sig) {

        Message topicExitMessageToSub =
            new TopicExitMessage(this.sender, "clientsub", sig.topic(), this.chatServerEntry);

        Message topicExitMessageToPull =
            new TopicExitMessage(this.sender, this.chatServerEntry.pullAddress(), sig.topic(), this.chatServerEntry);

        this.pushSCCarrier.sendMessage(topicExitMessageToPull);
        this.pubCarrier.sendMessageWithTopic(internalTopic, topicExitMessageToSub);

        this.reqSCSocket.disconnect(chatServerEntry.repAddress());
        this.pushSCSocket.disconnect(chatServerEntry.pullAddress());
        this.clientLog.shutdown();

        this.clientLog = null;
        this.currentTopic = null;
        this.chatServerEntry = null;
    }


    private void handleServeStateSignal(ServeStateSignal sig) {

        Message serverStateRequest = new ServerStateRequest(this.sender, this.chatServerEntry.repAddress());
        this.reqSCCarrier.sendMessage(serverStateRequest);

        ServerStateResponse response = (ServerStateResponse) this.reqSACarrier.receiveMessage();
        PrettyTable ptServerState = PrettyTable.fieldNames("Topic", "Users");

        response.getServerTotalState().forEach((topics, users) -> ptServerState.addRow(topics, users));
        System.out.println(ptServerState.toString());
    }


    private void handleChatMessageSignal(ChatMessageSignal sig) {
        this.pushSCCarrier.sendMessage(
            new ChatMessage(this.sender, this.chatServerEntry.pullAddress(), sig.topic(), sig.message(), sig.client()
        ));
    }


    private void handleLogSignal(LogSignal sig) {

        LogMessageRequest request = LogMessageRequest
            .newBuilder()
            .setHistory(sig.history())
            .build();

        this.clientLog.getLogs(request)
            .subscribe(
                item -> System.out.println(item.getMessage()),
                error -> error.printStackTrace(),
                () -> System.out.println("[ClientCon] log request completed")
            );
    }


    private void handleUserMessagesSignal(UserMessagesSignal sig) {

        UserMessagesRequest request = UserMessagesRequest
            .newBuilder()
            .setTopic(sig.topic())
            .setUsername(sig.client())
            .build();

        this.clientLog.getMessagesOfUser(request)
            .subscribe(
                item -> System.out.println(item.getMessage()),
                error -> error.printStackTrace(),
                () -> System.out.println("[ClientCon] user messages request completed")
        );
    }


    private void handleTimeoutSignal(TimeoutSignal sig) throws IOException {
        this.handleTopicExitSignal(new ExitTopicSignal(this.currentTopic));
        this.handleTopicEnterSignal(new EnterTopicSignal(this.currentTopic));
    }


    private void handleExitSignal(ExitSignal sig) throws IOException {

        this.pubCarrier.sendMessageWithTopic(
            this.internalTopic,
            new TopicExitMessage(this.sender, "clientsub", this.currentTopic, this.chatServerEntry));

        this.closeConnections();
        throw new IOException("[ClientCon] connections closed");
    }


    @Override
    public void run() {

        try {

            Signal signal;
            this.setupConnections();

            while ((signal = this.signals.take()) != null) {

                System.out.println("[ClientCon] received: " + signal);

                switch (signal) {
                    case LogSignal log -> handleLogSignal(log);
                    case ExitSignal exit -> handleExitSignal(exit);
                    case ExitTopicSignal exit -> handleTopicExitSignal(exit);
                    case TimeoutSignal timeout -> handleTimeoutSignal(timeout);
                    case ServeStateSignal state -> handleServeStateSignal(state);
                    case EnterTopicSignal enter -> handleTopicEnterSignal(enter);
                    case ChatMessageSignal chat -> handleChatMessageSignal(chat);
                    case UserMessagesSignal userReq -> handleUserMessagesSignal(userReq);
                    default -> throw new IllegalArgumentException("[ClientCon] unknown: " + signal);
                }
            }
        }

        catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}