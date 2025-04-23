package scatterchat.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.json.JSONObject;


public class Client{

    public static void main(String[] args) throws IOException, InterruptedException {

        final String configFilePath = args[0];
        final String nodeId = args[1];

        final String configFileContent = new String(Files.readAllBytes(Paths.get(configFilePath)));
        final JSONObject config = new JSONObject(configFileContent).getJSONObject(nodeId);

        Runnable clientUI = new ClientUI(config);
        Runnable clientSub = new ClientSub(config);

        List<Thread> workers = new ArrayList<>();
        ThreadFactory threadFactory = Thread.ofVirtual().factory();

        workers.add(threadFactory.newThread(clientUI));
        workers.add(threadFactory.newThread(clientSub));

        for (Thread worker : workers) {
            worker.start();
        }

        for (Thread worker : workers) {
            worker.join();
        }
    }
}