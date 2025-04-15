package scatterchat.chatserver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import org.json.JSONObject;
import scatterchat.protocol.messages.Message;


public class Main{
    
    public static void main(String[] args) throws IOException, InterruptedException{

        final String configFilePath = args[0];
        final String nodeId = args[1];

        final String configFileContent = new String(Files.readAllBytes(Paths.get(configFilePath)));
        final JSONObject nodeConfig = new JSONObject(configFileContent).getJSONObject(nodeId);

        BlockingQueue<Message> broadcast = new ArrayBlockingQueue<>(10);
        BlockingQueue<Message> delivered = new ArrayBlockingQueue<>(10);

        final String extPullAddress = nodeConfig.getString("extPullAddress");
        final String interPubAddress = nodeConfig.getString("interPubAddress");
        final String extPubAddress = nodeConfig.getString("extPubAddress");

        ChatServerExtPull chatServerExtPull = new ChatServerExtPull(extPullAddress, delivered, broadcast);
        ChatServerInterPub chatServerInterPub = new ChatServerInterPub(interPubAddress, broadcast);
        ChatServerInterSub chatServerInterSub = new ChatServerInterSub(nodeId, interPubAddress, delivered);
        ChatServerExtPub chatServerExtPub = new ChatServerExtPub(extPubAddress, delivered);

        List<Thread> workers = new ArrayList<>();
        ThreadFactory threadFactory = Thread.ofVirtual().factory();

        workers.add(threadFactory.newThread(chatServerExtPull));
        workers.add(threadFactory.newThread(chatServerInterPub));
        workers.add(threadFactory.newThread(chatServerInterSub));
        workers.add(threadFactory.newThread(chatServerExtPub));

        for (Thread worker : workers){
            worker.start();
        }

        for (Thread worker : workers){
            worker.join();
        }

        // TODO :: FALTA UMA THREAD PARA CORRER O GRPC
    }
}