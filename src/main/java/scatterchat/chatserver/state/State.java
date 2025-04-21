package scatterchat.chatserver.state;

import scatterchat.clock.VectorClock;
import scatterchat.crdt.ORSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;


public final class State {

    private String nodeId;
    private final Map<String, ORSet> usersORSetPerTopic;
    private final Map<String, VectorClock> clockPerTopic;
    private final Map<String, Set<String>> nodesPerTopic;

    public State(JSONObject config) {
        this.nodeId = config.getString("identity");
        this.clockPerTopic = new HashMap<>();
        this.nodesPerTopic = new HashMap<>();
        this.usersORSetPerTopic = new HashMap<>();
    }

    public String getNodeId() {
        return this.nodeId;
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

    public void setNodesOf(String topic, Set<String> nodes){
        this.nodesPerTopic.put(topic, nodes);
    }

    public void addUsersORSetOf(String topic) {
        this.usersORSetPerTopic.putIfAbsent(topic, new ORSet(nodeId));
    }

    public Map<String, Set<String>> getState() {
        return this.usersORSetPerTopic.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> entry.getValue().elements()));
    }
}