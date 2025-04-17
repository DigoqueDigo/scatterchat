package scatterchat.clock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class VectorClock {

    private Map<String, Integer> vector;

    public VectorClock() {
        this.vector = new HashMap<>();
    }

    public VectorClock(Set<String> nodes) {
        this.vector = nodes.stream().collect(Collectors.toMap(
            node -> node, 
            node -> 0));
    }

    public VectorClock(Map<String, Integer> vector) {
        this.vector = new HashMap<>(vector);
    }

    private VectorClock(VectorClock vectorClock) {
        this.vector = new HashMap<>(vectorClock.vector);
    }


    public VectorClock clone() {
        return new VectorClock(this);
    }

    public void putTimeOf(String node, Integer timestamp) {
        this.vector.put(node, timestamp);
    }

    public Integer getTimeOf(String node) {
        return this.vector.get(node);
    }

    public Set<String> getNodes() {
        return this.vector.keySet();
    }

    public String toString(){
        return this.vector.toString();
    }
}