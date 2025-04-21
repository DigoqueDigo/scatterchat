package scatterchat.aggrserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.json.JSONObject;

import scatterchat.aggrserver.state.State;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.aggr.AggrRep;
import scatterchat.protocol.message.cyclon.CyclonEntry;


public class AggrServer{

    public static void main(String[] args) throws IOException, InterruptedException {

        final String configFilePath = args[0];
        final String nodeId = args[1];

        final String configFileContent = new String(Files.readAllBytes(Paths.get(configFilePath)));
        final JSONObject config = new JSONObject(configFileContent).getJSONObject(nodeId);

        CyclonEntry entryPoint = null;
        State state = new State(config);

        if (config.has("entryPoint")) {
            entryPoint = new CyclonEntry(
                config.getJSONObject("entryPoint").getString("identity"),
                config.getJSONObject("entryPoint").getString("tcpExtPub"),
                config.getJSONObject("entryPoint").getString("tcpExtPubTimer"));
            state.setNeighbours(Arrays.asList(entryPoint));
        }

        BlockingQueue<AggrRep> response = new ArrayBlockingQueue<>(10);
        BlockingQueue<Message> received = new ArrayBlockingQueue<>(10);

        Runnable aggrServerReq = new AggrServerExtRep(config, received, response);
        Runnable aggrServerSub = new AggrServerExtSub(config, entryPoint, received);
        Runnable aggrServerPub = new AggrServerExtPub(config, state, received, response);
        Runnable aggrServerPubTimer = new AggrServerExtPubTimer(config, state);

        List<Thread> workers = new ArrayList<>();
        ThreadFactory threadFactory = Thread.ofVirtual().factory();

        workers.add(threadFactory.newThread(aggrServerReq));
        workers.add(threadFactory.newThread(aggrServerPub));
        workers.add(threadFactory.newThread(aggrServerSub));
        workers.add(threadFactory.newThread(aggrServerPubTimer));

        for (Thread worker : workers) {
            worker.start();
        }

        for (Thread worker : workers) {
            worker.join();
        }
    }
}