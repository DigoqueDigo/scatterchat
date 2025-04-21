package scatterchat.chatserver.state;

import scatterchat.clock.VectorClock;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatMessage;
import scatterchat.protocol.message.crtd.UserORSetMessage;

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

    private void addCausalMessage(String topic, CausalMessage message) {
        this.pending.putIfAbsent(topic, new ArrayList<>());
        this.pending.get(topic).add(message);
    }

    private void deliver(String topic) throws InterruptedException {

        synchronized (state) {

            int index = 0;
            final VectorClock vectorClock = state.getVectorClockOf(topic);
            final List<CausalMessage> pendingBuffer = pending.get(topic);

            while (index < pendingBuffer.size()) {

                boolean deliverable = true;
                final CausalMessage pendingMessage = pendingBuffer.get(index);

                final String sender = pendingMessage.getMessage().getSender();
                final VectorClock senderVectorClock = pendingMessage.getVectorClock();

                if (vectorClock.getTimeOf(sender) + 1 == senderVectorClock.getTimeOf(sender)) {

                    for (String node : vectorClock.getNodes()) {
                        if (!node.equals(sender) && vectorClock.getTimeOf(node) < vectorClock.getTimeOf(node)) {
                            deliverable = false;
                            break;
                        }
                    }

                    if (deliverable) {
                        pendingBuffer.remove(index);
                        delivered.put(pendingMessage.getMessage());
                        vectorClock.putTimeOf(sender, vectorClock.getTimeOf(sender));
                        state.setVectorClockOf(topic, vectorClock);
                        index = 0;
                    }
                }

                index++;
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {

                CausalMessage causalMessage = this.received.take();
                String topic = switch (causalMessage.getMessage()) {
                    case ChatMessage m -> m.getTopic();
                    case UserORSetMessage m -> m.getTopic();
                    default -> throw new Exception("Unknown message");
                };

                addCausalMessage(topic, causalMessage);
                deliver(topic);
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}