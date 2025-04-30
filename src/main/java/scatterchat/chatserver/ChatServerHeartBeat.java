package scatterchat.chatserver;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

import scatterchat.chatserver.state.State;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.chat.ChatServerEntry;
import scatterchat.protocol.message.chat.HeartBeatMessage;


public class ChatServerHeartBeat implements Runnable {

    public static final long DELAY = 0L;
    public static final long PERIOD = 500L;

    private State state;
    private BlockingQueue<Message> delivered;


    public ChatServerHeartBeat(State state, BlockingQueue<Message> delivered) {
        this.state = state;
        this.delivered = delivered;
    }


    @Override
    public void run() {

        try {

            synchronized (this.state) {

                ChatServerEntry nodeId = this.state.getNodeId();
                Set<String> topics = this.state.getServedTopics();

                for (String topic : topics) {
                    HeartBeatMessage heartBeat = new HeartBeatMessage(nodeId.pullAddress(), "client", topic);
                    this.delivered.put(heartBeat);
                }
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}