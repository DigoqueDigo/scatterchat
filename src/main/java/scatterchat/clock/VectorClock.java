package scatterchat.clock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import scatterchat.protocol.message.chat.ChatServerEntry;


public class VectorClock {

    private ChatServerEntry owner;
    private Map<ChatServerEntry, Integer> vector;

    public VectorClock() {
        this.vector = new HashMap<>();
    }

    public VectorClock(ChatServerEntry owner, Set<ChatServerEntry> nodesId) {
        this.owner = owner;
        this.vector = nodesId.stream()
            .collect(Collectors.toMap(
                node -> node, 
                node -> 0
        ));
    }

    private VectorClock(ChatServerEntry owner, Map<ChatServerEntry, Integer> vector) {
        this.owner = owner;
        this.vector = new HashMap<>(vector);
    }

    public VectorClock clone() {
        return new VectorClock(this.owner, this.vector);
    }

    public ChatServerEntry getOwner() {
        return this.owner;
    }

    public void putTimeOf(ChatServerEntry node, Integer time) {
        this.vector.put(node, time);
    }

    public Integer getTimeOf(ChatServerEntry node) {
        return this.vector.get(node);
    }

    public boolean isBefore(VectorClock vectorClock) {
        return this.vector.keySet()
            .stream()
            .allMatch(node -> getTimeOf(node) <= vectorClock.getTimeOf(node));
    }

    public Set<ChatServerEntry> getNodes() {
        return new HashSet<>(this.vector.keySet());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.owner);
        buffer.append(", ").append(this.vector);
        return buffer.toString();
    }
}