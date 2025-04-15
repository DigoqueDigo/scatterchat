package scatterchat.protocol.clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class VectorClock{

    private Map<String, Integer> vector;


    public VectorClock(){
        this.vector = new HashMap<>();
    }


    public VectorClock(List<String> nodes){
        this.vector = nodes.stream().collect(Collectors.toMap(x -> x, x -> 0));
    }


    public void putTimeOf(String node, Integer timestamp){
        this.vector.put(node, timestamp);
    }


    public Integer getTimeOf(String node){
        return this.vector.get(node);
    }


    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append("\t vector: " + this.vector);
        return buffer.toString();
    }
}