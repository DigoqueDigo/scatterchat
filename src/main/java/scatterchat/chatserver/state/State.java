package scatterchat.chatserver.state;

import scatterchat.clock.VectorClock;
import scatterchat.crdt.ORSet;
import scatterchat.protocol.message.chat.ChatServerEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;


public final class State {

    private ChatServerEntry nodeId;
    private final Map<String, ORSet> usersORSetPerTopic;
    private final Map<String, VectorClock> clockPerTopic;
    private final Map<String, Set<ChatServerEntry>> nodesPerTopic;

    public State(JSONObject config) {
        this.nodeId = new ChatServerEntry(config.getString("tcpExtRep"));
        this.clockPerTopic = new HashMap<>();
        this.nodesPerTopic = new HashMap<>();
        this.usersORSetPerTopic = new HashMap<>();
    }

    public ChatServerEntry getNodeId() {
        return this.nodeId;
    }

    public boolean hasTopic(String topic) {
        return this.nodesPerTopic.containsKey(topic);
    }

    public VectorClock getVectorClockOf(String topic) {
        return this.clockPerTopic.get(topic).clone();
    }

    public ORSet getUsersORSetOf(String topic) {
        return this.usersORSetPerTopic.get(topic);
    }
    
    public Set<String> getServedTopics() {
        return new HashSet<>(this.nodesPerTopic.keySet());
    }

    public void setVectorClockOf(String topic, VectorClock vectorClock) {
        this.clockPerTopic.put(topic, vectorClock);
    }

    public void registerServerNodes(String topic, Set<ChatServerEntry> nodes){
        this.nodesPerTopic.putIfAbsent(topic, nodes);
    }

    public void registerUsersORSet(String topic, ORSet orSet) {
        this.usersORSetPerTopic.putIfAbsent(topic, orSet);
    }

    public void registerVectorClock(String topic, VectorClock vectorClock) {
        this.clockPerTopic.putIfAbsent(topic, vectorClock);
    }

    public Map<String, Set<String>> getState() {
        return this.usersORSetPerTopic.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> entry.getValue().elements()));
    }
}