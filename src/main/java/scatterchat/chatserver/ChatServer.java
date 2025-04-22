package scatterchat.chatserver;

import org.json.JSONObject;
import scatterchat.chatserver.log.LogServer;
import scatterchat.chatserver.state.State;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.Message;
import scatterchat.chatserver.state.Deliver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;


public class ChatServer {

    public static void main(String[] args) throws IOException, InterruptedException {

        final String configFilePath = args[0];
        final String nodeId = args[1];

        final String configFileContent = new String(Files.readAllBytes(Paths.get(configFilePath)));
        final JSONObject config = new JSONObject(configFileContent).getJSONObject(nodeId);

        State state = new State(config);
        BlockingQueue<Message> broadcast = new ArrayBlockingQueue<>(10);
        BlockingQueue<Message> delivered = new ArrayBlockingQueue<>(10);
        BlockingQueue<CausalMessage> received = new ArrayBlockingQueue<>(10);

        Runnable chatServerExtRep = new ChatServerExtRep(config, state, broadcast);
        Runnable chatServerExtPull = new ChatServerExtPull(config, state, broadcast);
        Runnable chatServerInterPub = new ChatServerInterPub(config, state, broadcast);
        Runnable chatServerInterSub = new ChatServerInterSub(config, state, received);
        Runnable chatServerExtPub = new ChatServerExtPub(config, state, delivered);

        Runnable deliver = new Deliver(state, received, delivered);
        Runnable logServer = new LogServer(config); 

        List<Thread> workers = new ArrayList<>();
        ThreadFactory threadFactory = Thread.ofVirtual().factory();

        workers.add(threadFactory.newThread(chatServerExtRep));
        workers.add(threadFactory.newThread(chatServerExtPull));
        workers.add(threadFactory.newThread(chatServerInterPub));
        workers.add(threadFactory.newThread(chatServerInterSub));
        workers.add(threadFactory.newThread(chatServerExtPub));
        workers.add(threadFactory.newThread(deliver));
        workers.add(threadFactory.newThread(logServer));

        for (Thread worker : workers) {
            worker.start();
        }

        for (Thread worker : workers) {
            worker.join();
        }
    }
}