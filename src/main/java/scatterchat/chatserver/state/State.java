package scatterchat.chatserver.state;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import scatterchat.clock.VectorClock;
import scatterchat.crdt.CRDTClock;


public final class State{

    private String nodeId;
    private CRDTClock CRDTClockUsers;
    private Map<String, VectorClock> clockPerTopic;
    private Map<String, Set<String>> membersPerTopic;


    public State(String nodeId){
        this.nodeId = nodeId;
        this.clockPerTopic = new HashMap<>();
        this.membersPerTopic = new HashMap<>();
    } 


    public String getNodeId(){
        return this.nodeId;
    }


    public VectorClock getClockOf(String topic){
        return this.clockPerTopic.get(topic).clone();
    }


    public CRDTClock getCRDTClockUsers(){
        return this.CRDTClockUsers.clone();
    }


    public void incrementCRDTClockUsers(){
        this.CRDTClockUsers.increment();
    }


    public void setClockOf(String topic, VectorClock vectorClock){
        this.clockPerTopic.put(topic, vectorClock);
    }











    public void addUser(String topic, String user){
        this.membersPerTopic.putIfAbsent(topic, new HashSet<>());
        this.membersPerTopic.get(topic).add(user);
    }


    public void removeUser(String topic, String user){
        this.membersPerTopic.get(topic).remove(user);
    }


    public Set<String> getMembersOf(String topic){
        return this.membersPerTopic.get(topic);
    }


    public Map<String, Set<String>> getMembersPerTopic(){
        return this.membersPerTopic;
    }
}