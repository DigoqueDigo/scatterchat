package scatterchat.aggrserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.zeromq.ZContext;

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

        final State state = new State(config);
        final ZContext context = new ZContext();

        if (config.has("entryPoint")) {
            CyclonEntry entryPoint = new CyclonEntry(
                config.getJSONObject("entryPoint").getString("tcpExtPull"));
            state.setNeighbours(Arrays.asList(entryPoint));
        }

        BlockingQueue<Message> received = new ArrayBlockingQueue<>(10);
        BlockingQueue<AggrRep> responded = new ArrayBlockingQueue<>(10);
        BlockingQueue<Message> outBuffer = new ArrayBlockingQueue<>(10);

        Runnable aggrServerExtPush = new AggrServerExtPush(context, outBuffer);
        Runnable aggrServerExtPull = new AggrServerExtPull(config, context, received);
        Runnable aggrServerExtRep = new AggrServerExtRep(config, context, received, responded);
        Runnable aggrServerHandler = new AggrServerHandler(config, context, state, received, responded, outBuffer);
        Runnable aggrServerCyclonTimer = new AggrServerCyclonTimer(state, outBuffer);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
            aggrServerCyclonTimer,
            AggrServerCyclonTimer.DELAY,
            AggrServerCyclonTimer.PERIOD,
            TimeUnit.MILLISECONDS
        );

        List<Thread> workers = new ArrayList<>();
        ThreadFactory threadFactory = Thread.ofVirtual().factory();

        workers.add(threadFactory.newThread(aggrServerExtPush));
        workers.add(threadFactory.newThread(aggrServerExtPull));
        workers.add(threadFactory.newThread(aggrServerExtRep));
        workers.add(threadFactory.newThread(aggrServerHandler));
        

        for (Thread worker : workers) {
            worker.start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        context.close();
    }
}