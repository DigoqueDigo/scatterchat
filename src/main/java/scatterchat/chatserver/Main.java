package scatterchat.chatserver;

import org.json.JSONObject;
import scatterchat.chatserver.log.LogServer;
import scatterchat.protocol.messages.Message;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;


public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        final String configFilePath = args[0];
        final String nodeId = args[1];

        final String configFileContent = new String(Files.readAllBytes(Paths.get(configFilePath)));
        final JSONObject nodeConfig = new JSONObject(configFileContent).getJSONObject(nodeId);

        final int logServerPort = nodeConfig.getInt("logServerPort");
        final String extPubAddress = nodeConfig.getString("extPubAddress");
        final String extPullAddress = nodeConfig.getString("extPullAddress");
        final String interPubAddress = nodeConfig.getString("interPubAddress");
        final String logServerAddress = nodeConfig.getString("logServerAddress");

        BlockingQueue<Message> broadcast = new ArrayBlockingQueue<>(10);
        BlockingQueue<Message> received = new ArrayBlockingQueue<>(10);
        BlockingQueue<Message> delivered = new ArrayBlockingQueue<>(10);

        Runnable chatServerExtPull = new ChatServerExtPull(extPullAddress, delivered, broadcast);
        Runnable chatServerInterPub = new ChatServerInterPub(interPubAddress, broadcast);
        Runnable chatServerInterSub = new ChatServerInterSub(nodeId, interPubAddress, delivered);
        Runnable chatServerExtPub = new ChatServerExtPub(extPubAddress, delivered);
        Runnable logServer = new LogServer(logServerAddress, logServerPort);

        List<Thread> workers = new ArrayList<>();
        ThreadFactory threadFactory = Thread.ofVirtual().factory();

        workers.add(threadFactory.newThread(chatServerExtPull));
        workers.add(threadFactory.newThread(chatServerInterPub));
        workers.add(threadFactory.newThread(chatServerInterSub));
        workers.add(threadFactory.newThread(chatServerExtPub));
        workers.add(threadFactory.newThread(logServer));

        for (Thread worker : workers) {
            worker.start();
        }

        for (Thread worker : workers) {
            worker.join();
        }
    }
}