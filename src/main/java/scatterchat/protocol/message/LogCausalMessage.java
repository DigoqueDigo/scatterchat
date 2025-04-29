package scatterchat.protocol.message;

import scatterchat.clock.VectorClock;
import scatterchat.protocol.message.chat.ChatServerEntry;


public class LogCausalMessage implements Comparable<LogCausalMessage>{

    private long timestamp;
    private CausalMessage causalMessage;

    public LogCausalMessage(CausalMessage causalMessage) {
        this.timestamp = System.currentTimeMillis();
        this.causalMessage = causalMessage;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public CausalMessage getCausalMessage() {
        return this.causalMessage;
    }

    @Override
    public int compareTo(LogCausalMessage other) {
        VectorClock vc1 = this.causalMessage.getVectorClock();
        VectorClock vc2 = other.causalMessage.getVectorClock();

        if (vc1.isBefore(vc2))
            return -1;

        if (vc2.isBefore(vc1))
            return 1;

        ChatServerEntry owner1 = vc1.getOwner();
        ChatServerEntry owner2 = vc2.getOwner();
        return owner1.compareTo(owner2);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.timestamp);
        buffer.append(", ").append(this.causalMessage);
        return buffer.toString();
    }
}