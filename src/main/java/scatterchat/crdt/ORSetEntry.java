package scatterchat.crdt;

import scatterchat.protocol.message.chat.ChatServerEntry;


public record ORSetEntry(ChatServerEntry nodeId, int logicClock) {

    public ORSetEntry increment() {
        return new ORSetEntry(this.nodeId, this.logicClock + 1);
    }
}