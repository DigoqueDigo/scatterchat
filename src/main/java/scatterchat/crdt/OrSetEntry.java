package scatterchat.crdt;

import scatterchat.protocol.message.chat.ChatServerEntry;


public record OrSetEntry(ChatServerEntry nodeId, int logicClock) {

    public OrSetEntry increment() {
        return new OrSetEntry(this.nodeId, this.logicClock + 1);
    }
}