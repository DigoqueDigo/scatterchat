package scatterchat.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.JSONCarrier;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.aggr.AggrRep;
import scatterchat.protocol.message.aggr.AggrReq;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.message.dht.DHTGet;
import scatterchat.protocol.message.dht.DHTRep;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;


public class ClientUI implements Runnable {

    private JSONObject config;
    private ZContext context;

    private Terminal terminal;
    private LineReader lineReader;


    public ClientUI(JSONObject config, ZContext context) throws IOException {
        this.config = config;
        this.context = context;
        this.terminal = TerminalBuilder.builder().dumb(true).build();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }


    private String readTopic(String prompt) {

        String topic = null;
        List<String> invalidTopics = Arrays.asList(
            "/log", "/info", "/users", "/exit", ""
        );

        while (topic == null){

            topic = this.lineReader.readLine(prompt);
            topic = topic.strip();

            if (invalidTopics.contains(topic)){
                topic = null;
            }
        }

        return topic;
    }


    private String readMessage(String prompt) {
        String input = this.lineReader.readLine(prompt);
        return input.strip();
    }


    private DHTRep getTopic(DHTGet dhtGet, JSONCarrier carrier) throws IOException {
        System.out.println("[Client UI] sent: " + dhtGet);
        carrier.send(dhtGet);
        DHTRep dhtRep = carrier.receive();
        System.out.println("[Client UI] receive: " + dhtRep);
        return dhtRep;
    }


    private AggrRep reqAggregation(AggrReq request, ZMQCarrier carrier) {
        System.out.println("[Client UI] sent: " + request);
        carrier.sendMessage(request);
        AggrRep response = (AggrRep) carrier.receiveMessage();
        System.out.println("[Client UI] receive: " + response);
        return response;
    }


    private ServerStateResponse handleInfo(ServerStateRequest request, ZMQCarrier carrier) {
        System.out.println("[Client UI] sent: " + request);
        carrier.sendMessage(request);
        ServerStateResponse response = (ServerStateResponse) carrier.receiveMessage();
        System.out.println("[Client UI] receive: " + response);
        return response;
    }


    public void run(){

        try {

            Random random = new Random();
            Socket dhtSocket = new Socket();

            ZMQ.Socket pushSCSocket = this.context.createSocket(SocketType.PUSH);
            ZMQ.Socket reqSCSocket = this.context.createSocket(SocketType.REQ);
            ZMQ.Socket reqSASocket = this.context.createSocket(SocketType.REQ);
            ZMQ.Socket pubSocket = this.context.createSocket(SocketType.PUB);

            String sender = config.getString("username");
            String prompt = String.format("%s >>> ", sender);

            String inprocAddress = config.getString("inprocPubSub");
            String internalTopic = config.getString("internalTopic");

            int dhtPort = config.getJSONObject("dht").getInt("port");
            String dhtAddress = config.getJSONObject("dht").getString("address");
            String repSAAddress = config.getJSONObject("sa").getString("tcpExtRep");

            pubSocket.bind(inprocAddress);
            reqSASocket.connect(repSAAddress);
            dhtSocket.connect(new InetSocketAddress(dhtAddress, dhtPort));

            ZMQCarrier pushSCCarrier = new ZMQCarrier(pushSCSocket);
            ZMQCarrier reqSCCarrier = new ZMQCarrier(reqSCSocket);
            ZMQCarrier reqSACarrier = new ZMQCarrier(reqSASocket);
            ZMQCarrier pubCarrier = new ZMQCarrier(pubSocket);
            JSONCarrier dhtCarrier = new JSONCarrier(dhtSocket);

            System.out.println("[Client UI] bind: " + inprocAddress);
            System.out.println("[Client UI] connect: " + repSAAddress);
            System.out.println("[Client UI] connnect: " + dhtAddress + ":" + dhtPort);

            try {

                while (true) {

                    String message;
                    String topic = readTopic(prompt);
                    String topicPrompt = String.format("[%s] %s", topic, prompt);
                    DHTRep dhtRep = getTopic(new DHTGet(0, topic), dhtCarrier);

                    while (dhtRep.ips().isEmpty()) {    
                        reqAggregation(new AggrReq(sender, repSAAddress, topic), reqSACarrier);
                        dhtRep = getTopic(new DHTGet(0, topic), dhtCarrier);
                    }

                    int scIndex = random.nextInt(dhtRep.ips().size());
                    ChatServerEntry chatServerEntry = new ChatServerEntry(dhtRep.ips().get(scIndex));

                    reqSCSocket.connect(chatServerEntry.repAddress());
                    pushSCSocket.connect(chatServerEntry.pullAddress());

                    Message topicEnterMessageToSub = new TopicEnterMessage(sender, "client sub", topic, chatServerEntry);
                    Message topicEnterMessageToPull = new TopicEnterMessage(sender, chatServerEntry.pullAddress(), topic, chatServerEntry);

                    pushSCCarrier.sendMessage(topicEnterMessageToPull);
                    pubCarrier.sendMessageWithTopic(internalTopic, topicEnterMessageToSub);                

                    while (!(message = readMessage(topicPrompt)).equals("/exit")){

                        if (message.startsWith("/info") || message.startsWith("/users")) {
                            ServerStateRequest request = new ServerStateRequest(sender, chatServerEntry.repAddress());
                            handleInfo(request, reqSCCarrier);
                        } else {
                            Message chatMessage = new ChatMessage(sender, chatServerEntry.pullAddress(), topic, message, sender);
                            pushSCCarrier.sendMessage(chatMessage);
                        }
                    }

                    Message topicExitMessageToSub = new TopicExitMessage(sender, "client sub", topic, chatServerEntry);
                    Message topicExitMessageToPull = new TopicExitMessage(sender, chatServerEntry.pullAddress(), topic, chatServerEntry);

                    pushSCCarrier.sendMessage(topicExitMessageToPull);
                    pubCarrier.sendMessageWithTopic(internalTopic, topicExitMessageToSub);

                    reqSCSocket.disconnect(chatServerEntry.repAddress());
                    pushSCSocket.disconnect(chatServerEntry.pullAddress());
                }
            }

            catch (EndOfFileException e){

                Message topicExitMessageToSub = new TopicExitMessage(sender, "client sub", null, null);
                pubCarrier.sendMessageWithTopic(internalTopic, topicExitMessageToSub);

                dhtSocket.close();
                pubSocket.close();
                reqSCSocket.close();
                reqSASocket.close();
                pushSCSocket.close();

                System.out.println("Bye");
            }
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}