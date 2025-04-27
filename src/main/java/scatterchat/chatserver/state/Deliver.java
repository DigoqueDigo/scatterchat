package scatterchat.chatserver.state;

import scatterchat.clock.VectorClock;
import scatterchat.protocol.message.CausalMessage;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatServerEntry;

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
        String topic = message.getTopic();
        this.pending.putIfAbsent(topic, new ArrayList<>());
        this.pending.get(topic).add(message);
    }


    private void deliver(String topic) throws InterruptedException {

        synchronized (this.state) {

            int index = 0;
            List<CausalMessage> pendingTopicBuffer = pending.get(topic);
            System.out.println(this.state);

            while (index < pendingTopicBuffer.size()) {

                CausalMessage pendingMessage = pendingTopicBuffer.get(index);
                VectorClock localVectorClock = state.getVectorClockOf(topic);
                VectorClock senderVectorClock = pendingMessage.getVectorClock();

                System.out.println("pendingMessage " + pendingMessage);
                System.out.println("localVectorClock " + localVectorClock);
                System.out.println("senderVectorClock " + senderVectorClock);

                ChatServerEntry localId = state.getNodeId();
                ChatServerEntry senderId = senderVectorClock.getOwner();

                System.out.println("localId " + localId);
                System.out.println("senderId " + senderId);

                boolean imSender = localId.equals(senderId);
                boolean immediatelyPrev = localVectorClock.getTimeOf(senderId) + 1 == senderVectorClock.getTimeOf(senderId);
                boolean allUpdate = localVectorClock.getNodes()
                    .stream()
                    .filter(node -> !node.equals(senderId))
                    .allMatch(node -> localVectorClock.getTimeOf(node) >= senderVectorClock.getTimeOf(node));

                if (imSender || (immediatelyPrev && allUpdate)) {

                    pendingMessage = pendingTopicBuffer.remove(index);
                    this.delivered.put(pendingMessage.getMessage());
                    index = 0;

                    if (!imSender) {
                        localVectorClock.putTimeOf(senderId, senderVectorClock.getTimeOf(senderId));
                        this.state.setVectorClockOf(topic, localVectorClock);
                    }

                    System.out.println(this.state);
                }

                index++;
            }
        }
    }


    @Override
    public void run() {

        try {

            CausalMessage causalMessage;
            System.out.println("[SC deviler] started");

            while ((causalMessage = this.received.take()) != null) {
                System.out.println("[SC deliver] received: " + causalMessage);
                addCausalMessage(causalMessage);
                deliver(causalMessage.getTopic());
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}