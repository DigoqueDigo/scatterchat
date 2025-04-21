package scatterchat.aggrserver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.aggrserver.state.State;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.cyclon.CyclonEntry;
import scatterchat.protocol.message.cyclon.CyclonMessage;


public class AggrServerCyclonTimer implements Runnable {

    private static final Long DELAY = 1000L;
    private static final Long LOWER_BOUND_RATE = 5000L;
    private static final Long UPPER_BOUND_RATE = 10000L;

    private State state;
    private Random random;
    private ScheduledExecutorService executor;


    public AggrServerCyclonTimer(State state){
        this.state = state;
        this.random = new Random();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }


    @Override
    public void run() {

        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.ROUTER);
        ZMQCarrier carrier = new ZMQCarrier(socket);

        executor.scheduleAtFixedRate(() -> {

            synchronized (state) {

                state.setCyclonOnGoing(true);
                List<CyclonEntry> neighbours = state.getNeighbours();
                int subSetLength = Math.min(neighbours.size(), state.getShuffleLength());

                Collections.shuffle(neighbours);
                List<CyclonEntry> subSet = neighbours.subList(0, subSetLength);

                int targetIndex = random.nextInt(subSetLength);
                CyclonEntry target = subSet.remove(targetIndex);

                CyclonEntry mCyclonEntry = state.getCyclonEntry();
                subSet.add(mCyclonEntry);

                CyclonMessage cyclonMessage = new CyclonMessage(
                    mCyclonEntry.address(),
                    mCyclonEntry.identity(),
                    new HashSet<>(subSet)
                );

                socket.connect(target.address());
                carrier.sendMessageWithIdentity(target.identity(), cyclonMessage);
                state.setNodesSent(subSet);
            }

        }, DELAY, random.nextLong(LOWER_BOUND_RATE, UPPER_BOUND_RATE), TimeUnit.MILLISECONDS);

        socket.close();
        context.close();
    }
}