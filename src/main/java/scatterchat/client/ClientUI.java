package scatterchat.client;

import java.io.IOException;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.chat.TopicEnterMessage;
import scatterchat.protocol.message.chat.TopicExitMessage;
import scatterchat.protocol.message.info.ServerStateRequest;


public class ClientUI implements Runnable{

    private JSONObject config;
    private Terminal terminal;
    private LineReader lineReader;


    public ClientUI(JSONObject config) throws IOException{
        this.config = config;
        this.terminal = TerminalBuilder.builder().dumb(true).build();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }


    private String readTopic(String prompt){

        String topic = null;

        while (topic == null){

            topic = this.lineReader.readLine(prompt);
            topic = topic.strip();

            if (topic.equals("[internal]") || topic.startsWith("/")){
                System.out.println("Invalid topic: " + topic);
                topic = null;
            }
        }

        return topic;
    }


    private String readMessage(){
        return this.lineReader.readLine().strip();
    }


    public void run(){

        final String topicPrompt = new AttributedString("Enter Topic >> ",
            AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.WHITE)).toAnsi();

        final String bye = new AttributedString("Bye!",
            AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.WHITE)).toAnsi();

        ZContext context = new ZContext();
        ZMQ.Socket pushSocket = context.createSocket(SocketType.PUSH);
        ZMQ.Socket reqSocket = context.createSocket(SocketType.REQ);
        ZMQ.Socket pubSocket = context.createSocket(SocketType.PUB);

        ZMQCarrier pushCarrier = new ZMQCarrier(pushSocket);
        ZMQCarrier pubCarrier = new ZMQCarrier(pubSocket);
        ZMQCarrier reqCarrier = new ZMQCarrier(reqSocket);

        final String sender = config.getString("identity");
        pubSocket.bind(config.getString("inprocPubSub"));

        try{

            while (true){

                String message;
                String topic = readTopic(topicPrompt);

                String chatServerPullAddress = "";
                String chatServerPubAddress = "";
                String chatServerRepAddress = "";

                reqSocket.connect(chatServerRepAddress);
                pushSocket.connect(chatServerPullAddress);

                Message topicEnterMessageToPull = new TopicEnterMessage(sender, topic);
                Message topicEnterMessageToSub = new TopicEnterMessage(sender, topic, chatServerPubAddress);

                pushCarrier.sendMessage(topicEnterMessageToPull);
                pubCarrier.sendMessageWithTopic("[internal]", topicEnterMessageToSub);                

                while (!(message = readMessage()).equals("/exit")){

                    if (message.startsWith("/info")){
                        reqCarrier.sendMessage(new ServerStateRequest());
                        System.out.println(reqCarrier.receiveMessage());
                    }

                    else {
                        Message chatMessage = new ChatMessage(sender, topic, message);
                        pushCarrier.sendMessage(chatMessage);
                    }
                }

                Message topicExitMessageToPull = new TopicExitMessage(sender, topic);
                Message topicExitMessageToSub = new TopicExitMessage(sender, topic, chatServerPubAddress);

                pushCarrier.sendMessage(topicExitMessageToPull);
                pubCarrier.sendMessageWithTopic("[internal]", topicExitMessageToSub);

                reqSocket.disconnect(chatServerRepAddress);
                pushSocket.disconnect(chatServerPullAddress);
            }
        }

        catch (EndOfFileException e){

            Message topicExitMessageToSub = new TopicExitMessage(sender, null);
            pubCarrier.sendMessageWithTopic("[internal]", topicExitMessageToSub);

            pushSocket.close();
            reqSocket.close();
            pubSocket.close();
            context.close();

            System.out.println(bye);
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}