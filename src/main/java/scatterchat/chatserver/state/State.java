package scatterchat.chatserver.state;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import scatterchat.clock.VectorClock;


public final class State{

    private String nodeId;
    private Map<String, ORSet> usersORSetPerTopic;
    private Map<String, VectorClock> clockPerTopic;
    private Map<String, Set<String>> nodesPerTopic;


    public State(String nodeId){
        this.nodeId = nodeId;
        this.clockPerTopic = new HashMap<>();
        this.nodesPerTopic = new HashMap<>();
        this.usersORSetPerTopic = new HashMap<>();
    }


    public String getNodeId(){
        return this.nodeId;
    }


    public VectorClock getVectorClockOf(String topic){
        return this.clockPerTopic.get(topic).clone();
    }


    public void setVectorClockOf(String topic, VectorClock vectorClock){
        this.clockPerTopic.put(topic, vectorClock);
    }


    public ORSet getUsersORSetOf(String topic){
        return this.usersORSetPerTopic.get(topic);
    }


    public Set<String> getNodesOfTopic(String topic){
        return this.nodesPerTopic.get(topic);
    }
}