package scatterchat.chatserver.state;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class State{

    private Map<String, List<String>> groups;


    public State(){
        this.groups = new HashMap<>();
    }   
}