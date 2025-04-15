package scatterchat.chatserver.deliver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import scatterchat.protocol.clock.VectorClock;
import scatterchat.protocol.messages.ChatMessage;
import scatterchat.protocol.messages.Message;


public class Deliver{

    /*
     * chat1 -> myclock1
     * chat2 -> myclock2
     * 
     * chat1 -> [m1, m3, m2]
     * chat2 -> [m4, m5, m6]
     * 
     * chat1 -> [sc1, sc2, sc3]
     * chat2 -> [sc3, sc5, sc6]
     */

    private Map<String, VectorClock> clocks;
    private Map<String, List<ChatMessage>> pending;
    private Map<String, List<String>> members;
    private BlockingQueue<Message> delivered;


    public Deliver(BlockingQueue<Message> delivered){
        this.clocks = new HashMap<>();
        this.pending = new HashMap<>();
        this.members = new HashMap<>();
        this.delivered = delivered;
    }


    public void putClock(String chat, VectorClock clock){
        this.clocks.put(chat, clock);
    }


    public void addPendingMessage(String chat, ChatMessage chatMessage){
        this.pending.putIfAbsent(chat, new ArrayList<>());
        this.pending.get(chat).add(chatMessage);
    }


    public void putChatMembers(String chat, List<String> members) throws InterruptedException{
        this.members.putIfAbsent(chat, members);
        this.deliver(chat);
    }


    private void deliver(String chat) throws InterruptedException{

        int index = 0;
        VectorClock clock = this.clocks.get(chat);
        List<ChatMessage> pendingBuffer = this.pending.get(chat);

        while (index < pendingBuffer.size()){

            boolean deliverable = true;
            ChatMessage pendingMessage = pendingBuffer.get(index);

            String sender = pendingMessage.getSender();
            VectorClock messageClock = pendingMessage.getVectorClock();

            if (clock.getTimeOf(sender) + 1 == messageClock.getTimeOf(sender)){

                for (String node : this.members.get(chat)){
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