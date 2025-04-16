package scatterchat.clock;
import java.util.HashMap;
import java.util.Map;


public class VectorClock{

    private Map<String, Integer> vector;


    public VectorClock(Map<String, Integer> vector){
        this.vector = new HashMap<>(vector);
    }


    private VectorClock(VectorClock vectorClock){
        this.vector = new HashMap<>(vectorClock.vector);
    }


    public VectorClock clone(){
        return new VectorClock(this);
    }


    public void putTimeOf(String node, Integer timestamp){
        this.vector.put(node, timestamp);
    }


    public Integer getTimeOf(String node){
        return this.vector.get(node);
    }
}