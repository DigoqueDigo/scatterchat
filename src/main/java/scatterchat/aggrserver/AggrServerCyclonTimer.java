package scatterchat.aggrserver;

import java.util.Collections;
import java.util.Random;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import scatterchat.aggrserver.state.State;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.cyclon.CyclonEntry;
import scatterchat.protocol.message.cyclon.CyclonMessage;


public class AggrServerCyclonTimer implements Runnable {

    public static final long DELAY = 1_000L;
    public static final long PERIOD = 5_000L;

    private State state;
    private Random random;
    private BlockingQueue<Message> outBuffer;


    public AggrServerCyclonTimer(State state, BlockingQueue<Message> outBuffer){
        this.state = state;
        this.outBuffer = outBuffer;
        this.random = new Random();
    }
    
    
    @Override
    public void run() {

        try {

            synchronized (this.state) {

                System.out.println("[AggrServeCyclonTimer] start");
                System.out.println(state);

                if (state.getNeighbours().size() > 0) {

                    List<CyclonEntry> neighbours = state.getNeighbours();
                    Collections.shuffle(neighbours);                

                    int subSetLength = Math.min(neighbours.size(), State.CYCLON_SHUFFLE_LENGTH);
                    List<CyclonEntry> subSet = neighbours.subList(0, subSetLength);

                    int targetIndex = random.nextInt(subSetLength);
                    CyclonEntry target = subSet.remove(targetIndex);
                    CyclonEntry sender = state.getMyCyclonEntry();

                    subSet.add(0, sender);
                    System.out.println(subSetLength);
                    System.out.println(subSet);

                    CyclonMessage cyclonMessage = new CyclonMessage(
                        sender.pullAddress(),
                        target.pullAddress(),
                        subSet
                    );

                    System.out.println(cyclonMessage);
                    System.out.println("TARGET: " + target);

                    state.setNodesSent(subSet);
                    state.setCyclonOnGoing(true);

                    System.out.println("[AggrServeCyclonTimer] sent: " + cyclonMessage);
                    this.outBuffer.put(cyclonMessage);
                    System.out.println(state);
                }
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}