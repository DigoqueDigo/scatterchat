package scatterchat.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.aggr.AggrRep;
import scatterchat.protocol.message.aggr.AggrReq;
import scatterchat.protocol.message.chat.TopicExitMessage;


public class ClientUI implements Runnable{

    private JSONObject config;
    private Terminal terminal;
    private LineReader lineReader;


    public ClientUI(JSONObject config) throws IOException{
        this.config = config;
        this.terminal = TerminalBuilder.builder().dumb(true).build();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }


    private String readTopic(Set<String> invalidTopics){

        String topic = null;

        while (topic == null){

            topic = this.lineReader.readLine("client >>> ");
            topic = topic.strip();

            if (invalidTopics.contains(topic)){
                System.out.println("Invalid topic: " + topic);
                topic = null;
            }
        }

        return topic;
    }


    // private String readMessage(){
    //     return this.lineReader.readLine().strip();
    // }


    public void run(){

        ZContext context = new ZContext();
        ZMQ.Socket pushSocket = context.createSocket(SocketType.PUSH);
        ZMQ.Socket reqSocket = context.createSocket(SocketType.REQ);
        ZMQ.Socket pubSocket = context.createSocket(SocketType.PUB);

     //   ZMQCarrier pushCarrier = new ZMQCarrier(pushSocket);
        ZMQCarrier pubCarrier = new ZMQCarrier(pubSocket);
        ZMQCarrier reqCarrier = new ZMQCarrier(reqSocket);

        String sender = config.getString("username");
        String inprocAddress = config.getString("inprocPubSub");
        String internalTopic = config.getString("internalTopic");
        String saRepAddress = config.getJSONObject("sa").getString("tcpExtRep");

        pubSocket.bind(inprocAddress);
        reqSocket.connect(saRepAddress);

        System.out.println("[Client UI] bind: " + inprocAddress);
        System.out.println("[Client UI] connect: " + saRepAddress);

        Set<String> invalidTopic = new HashSet<>(Arrays.asList(
            internalTopic, "/log", "/info", "/exit", ""
        ));

        try{

            while (true){

             //   String message;

                String topic = readTopic(invalidTopic);

                AggrReq aggrReq = new AggrReq(sender, saRepAddress, topic);
                reqCarrier.sendMessage(aggrReq);

                Message message = reqCarrier.receiveMessage();
                System.out.println(message);
                AggrRep aggrRep = (AggrRep) message;
                System.out.println("[Client UI] received: " + aggrRep);

                break;

                // String chatServerExtRepAddress = "";
                // ChatServerEntry chatServerEntry = new ChatServerEntry(chatServerExtRepAddress);

                // reqSocket.connect(chatServerEntry.repAddress());
                // pushSocket.connect(chatServerEntry.pullAddress());

                // Message topicEnterMessageToSub = new TopicEnterMessage(sender, "client sub", topic, chatServerEntry);
                // Message topicEnterMessageToPull = new TopicEnterMessage(sender, chatServerEntry.pullAddress(), topic, chatServerEntry);

                // pushCarrier.sendMessage(topicEnterMessageToPull);
                // pubCarrier.sendMessageWithTopic(internalTopic, topicEnterMessageToSub);                

                // while (!(message = readMessage()).equals("/exit")){

                //     if (message.startsWith("/info")){
                //         Message serverStateRequest = new ServerStateRequest(sender, chatServerEntry.repAddress());
                //         reqCarrier.sendMessage(serverStateRequest);
                //         Message serverStateResponse = (ServerStateResponse) reqCarrier.receiveMessage();
                //         System.out.println(serverStateResponse);
                //     }

                //     else {
                //         Message chatMessage = new ChatMessage(sender, chatServerEntry.pullAddress(), topic, message, sender);
                //         pushCarrier.sendMessage(chatMessage);
                //     }
                // }

                // Message topicExitMessageToSub = new TopicExitMessage(sender, "client sub", topic, chatServerEntry);
                // Message topicExitMessageToPull = new TopicExitMessage(sender, chatServerEntry.pullAddress(), topic, chatServerEntry);

                // pushCarrier.sendMessage(topicExitMessageToPull);
                // pubCarrier.sendMessageWithTopic(internalTopic, topicExitMessageToSub);

                // reqSocket.disconnect(chatServerEntry.repAddress());
                // pushSocket.disconnect(chatServerEntry.pullAddress());
            }
        }

        catch (EndOfFileException e){

            Message topicExitMessageToSub = new TopicExitMessage(sender, "client sub", null, null);
            pubCarrier.sendMessageWithTopic(internalTopic, topicExitMessageToSub);

            pushSocket.close();
            reqSocket.close();
            pubSocket.close();
            context.close();

            System.out.println("Bye");
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}