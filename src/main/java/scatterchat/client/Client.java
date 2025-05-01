package scatterchat.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.json.JSONObject;
import org.zeromq.ZContext;

import javafx.application.Application;
import scatterchat.protocol.signal.Signal;


public class Client{

    public static void main(String[] args) throws IOException, InterruptedException {

        final String configFilePath = args[0];
        final String nodeId = args[1];

        final ZContext context = new ZContext();
        final String configFileContent = new String(Files.readAllBytes(Paths.get(configFilePath)));
        final JSONObject config = new JSONObject(configFileContent).getJSONObject(nodeId);

        final BlockingQueue<Signal> signals = new ArrayBlockingQueue<>(10);

        Runnable clientCon = new ClientCon(config, context, signals);
        Runnable clientSub = new ClientSub(config, context, signals);

        List<Thread> workers = new ArrayList<>();
        ThreadFactory threadFactory = Thread.ofVirtual().factory();

        workers.add(threadFactory.newThread(clientCon));
        workers.add(threadFactory.newThread(clientSub));

        for (Thread worker : workers) {
            worker.start();
        }

        ClientUI.setParameters(config, signals);
        Application.launch(ClientUI.class, args);

        for (Thread worker : workers) {
            worker.join();
        }

        context.close();
    }
}