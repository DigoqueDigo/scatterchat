package scatterchat.chatserver.state;

import scatterchat.clock.VectorClock;
import scatterchat.protocol.messages.CausalMessage;
import scatterchat.protocol.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;


public class Deliver implements Runnable {

    private final State state;
    private final BlockingQueue<Message> delivered;
    private final BlockingQueue<CausalMessage> received;
    private final Map<String, List<CausalMessage>> pending;

    public Deliver(State state, BlockingQueue<CausalMessage> received, BlockingQueue<Message> delivered) {
        this.state = state;
        this.received = received;
        this.delivered = delivered;
        this.pending = new HashMap<>();
    }

    private void addCausalMessage(CausalMessage message) {
        String topic = message.getMessage().getTopic();
        this.pending.putIfAbsent(topic, new ArrayList<>());
        this.pending.get(topic).add(message);
    }

    private void deliver(String topic) throws InterruptedException {
        synchronized (state) {
            int index = 0;
            final VectorClock clock = state.getVectorClockOf(topic);
            final List<CausalMessage> pendingBuffer = pending.get(topic);

            while (index < pendingBuffer.size()) {

                boolean deliverable = true;
                final CausalMessage pendingMessage = pendingBuffer.get(index);

                final String sender = pendingMessage.getMessage().getSender();
                final VectorClock messageClock = pendingMessage.getVectorClock();

                if (clock.getTimeOf(sender) + 1 == messageClock.getTimeOf(sender)) {

                    for (String node : state.getNodesOfTopic(topic)) {
                        if (!node.equals(sender) && clock.getTimeOf(node) < messageClock.getTimeOf(node)) {
                            deliverable = false;
                            break;
                        }
                    }

                    if (deliverable) {
                        pendingBuffer.remove(index);
                        delivered.put(pendingMessage.getMessage());
                        clock.putTimeOf(sender, messageClock.getTimeOf(sender));
                        index = 0;
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                CausalMessage message = this.received.take();
                final String topic = message.getMessage().getTopic();
                addCausalMessage(message);
                deliver(topic);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}