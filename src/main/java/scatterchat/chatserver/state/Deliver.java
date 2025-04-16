package scatterchat.chatserver.state;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import scatterchat.clock.VectorClock;
import scatterchat.protocol.messages.CausalMessage;
import scatterchat.protocol.messages.Message;


public class Deliver implements Runnable{

    private State state;
    private BlockingQueue<Message> delivered;
    private BlockingQueue<CausalMessage> received;
    private Map<String, List<CausalMessage>> pending;


    public Deliver(State state, BlockingQueue<CausalMessage> received, BlockingQueue<Message> delivered){
        this.state = state;
        this.received = received;
        this.delivered = delivered;
        this.pending = new HashMap<>();
    }


    private void addCausalMessage(CausalMessage message){
        String topic = message.getTopic();
        this.pending.putIfAbsent(topic, new ArrayList<>());
        this.pending.get(topic).add(message);
    }


    private void deliver(String topic) throws InterruptedException{

        synchronized (state){

            int index = 0;
            VectorClock clock = state.getClockOf(topic);
            List<CausalMessage> pendingBuffer = pending.get(topic);

            while (index < pendingBuffer.size()){

                boolean deliverable = true;
                CausalMessage pendingMessage = pendingBuffer.get(index);

                String sender = pendingMessage.getSender();
                VectorClock messageClock = pendingMessage.getVectorClock();

                if (clock.getTimeOf(sender) + 1 == messageClock.getTimeOf(sender)){

                    for (String node : state.getMembersOf(topic)){
                        if (!node.equals(sender) && clock.getTimeOf(node) < messageClock.getTimeOf(node)){
                            deliverable = false;
                            break;
                        }
                    }

                    if (deliverable){
                        pendingBuffer.remove(index);
                        delivered.put(pendingMessage);
                        clock.putTimeOf(sender, messageClock.getTimeOf(sender));
                        index = 0;
                    }
                }
            }
        }
    }


    @Override
    public void run(){

        try{
            CausalMessage message = null;
            while ((message = this.received.take()) != null){
                String topic = message.getTopic();
                addCausalMessage(message);
                deliver(topic);
            }
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}