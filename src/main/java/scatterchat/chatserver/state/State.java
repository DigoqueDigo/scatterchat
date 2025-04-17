package scatterchat.chatserver.state;

import scatterchat.clock.VectorClock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class State {

    private final String nodeId;
    private final Map<String, ORSet> usersORSetPerTopic;
    private final Map<String, VectorClock> clockPerTopic;
    private final Map<String, Set<String>> nodesPerTopic;

    public State(String nodeId) {
        this.nodeId = nodeId;
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

    public void setVectorClockOf(String topic, VectorClock vectorClock) {
        this.clockPerTopic.put(topic, vectorClock);
    }

    public ORSet getUsersORSetOf(String topic) {
        return this.usersORSetPerTopic.get(topic);
    }

    public Set<String> getNodesOfTopic(String topic) {
        return this.nodesPerTopic.get(topic);
    }

    public Set<String> getServedTopics() {
        return new HashSet<>(this.nodesPerTopic.keySet());
    }
}