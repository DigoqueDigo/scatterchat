package scatterchat.aggrserver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.aggrserver.state.State;
import scatterchat.protocol.carrier.ZMQCarrier;
import scatterchat.protocol.message.cyclon.CyclonEntry;
import scatterchat.protocol.message.cyclon.CyclonMessage;


public class AggrServerExtPubTimer implements Runnable {

    private static final Long DELAY = 1000L;
    private static final Long LOWER_BOUND_RATE = 3000L;
    private static final Long UPPER_BOUND_RATE = 5000L;

    private State state;
    private Random random;
    private JSONObject config;

    private ZContext context;
    private ZMQ.Socket socket;
    private ZMQCarrier carrier;

    private ScheduledExecutorService executor;


    public AggrServerExtPubTimer(JSONObject config, State state){
        this.state = state;
        this.config = config;
        this.random = new Random();
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.carrier = null;
        this.socket = null;
        this.carrier = null;
    }
    
    
    @Override
    public void run() {

        this.context = new ZContext();
        this.socket = context.createSocket(SocketType.SUB);
        this.carrier = new ZMQCarrier(socket);

        String address = config.getString("tcpExtPubTimer");
        this.socket.bind(address);
        System.out.println("[AggrServerPubTimer] Bind" + address);

        executor.scheduleAtFixedRate(() -> {

            try {

                synchronized (state) {

                    System.out.println("[AggrServerPubTimer] Start execution");
                    System.out.println(state);

                    if (state.getNeighbours().size() > 0) {

                        state.setCyclonOnGoing(true);
                        List<CyclonEntry> neighbours = state.getNeighbours();
                        int subSetLength = Math.min(neighbours.size(), State.CyclonShuffleLength);

                        Collections.shuffle(neighbours);
                        List<CyclonEntry> subSet = neighbours.subList(0, subSetLength);

                        System.out.println(subSetLength);
                        System.out.println(subSet);

                        int targetIndex = random.nextInt(subSetLength);
                        CyclonEntry target = subSet.remove(targetIndex);

                        CyclonEntry mCyclonEntry = state.getMyCyclonEntry();
                        subSet.add(mCyclonEntry);

                        CyclonMessage cyclonMessage = new CyclonMessage(
                            mCyclonEntry.pubTimerAddress(),
                            mCyclonEntry.identity(),
                            new HashSet<>(subSet)
                        );

                        System.out.println(cyclonMessage);
                        System.out.println("TARGET: " + target);

                        carrier.sendMessageWithTopic(target.identity(), cyclonMessage);
                        state.setNodesSent(subSet);

                        System.out.println(state);
                        System.out.println("[AggrServeCyclonTimer] Sent: " + cyclonMessage);
                    }
                }
            }

            catch (Exception e) {
                e.printStackTrace();
            }

        }, DELAY, random.nextLong(LOWER_BOUND_RATE, UPPER_BOUND_RATE), TimeUnit.MILLISECONDS);
    }
}