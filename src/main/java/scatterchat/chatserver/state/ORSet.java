package scatterchat.chatserver.state;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import scatterchat.crdt.CRDTEntry;
import scatterchat.protocol.messages.crtd.ORSetMessage;
import scatterchat.protocol.messages.crtd.ORSetMessage.OPERATION;


public class ORSet{

    private CRDTEntry clock;
    private HashMap<String, Set<CRDTEntry>> store;


    public ORSet(String nodeId){
        this.store = new HashMap<>();
        this.clock = new CRDTEntry(nodeId, 0);
    }
    

    private ORSetMessage prepareAdd(OPERATION operation, String element){
        this.clock.increment();
        this.store.putIfAbsent(element, new HashSet<>());
        Set<CRDTEntry> entries = this.store.get(element).stream().map(x -> x.clone()).collect(Collectors.toSet());
        return new ORSetMessage(operation, element, clock.clone(), entries);
    }


    private ORSetMessage prepareRemove(OPERATION operation, String element){
        this.store.putIfAbsent(element, new HashSet<>());
        Set<CRDTEntry> entries = this.store.get(element).stream().map(x -> x.clone()).collect(Collectors.toSet());
        return new ORSetMessage(operation, element, entries);
    }


    private void effectADD(ORSetMessage message){

        String element = message.getElement();
        this.store.putIfAbsent(element, new HashSet<>());

        Set<CRDTEntry> entries = this.store.get(element);
        entries.removeAll(message.getEntries());
        entries.add(message.getClock());
    }


    private void effectREMOVE(ORSetMessage message){

        String element = message.getElement();
        Set<CRDTEntry> entries = this.store.get(element);
        entries.removeAll(message.getEntries());

        if (entries.size() == 0){
            this.store.remove(element);
        }
    }


    public ORSetMessage prepare(OPERATION operation, String element){
        return switch (operation){
            case ADD -> prepareAdd(operation, element);
            case REMOVE -> prepareRemove(operation, element);
            default -> null;
        };
    }


    public void effect(ORSetMessage message){
        switch (message.getOperation()){
            case ADD -> effectADD(message);
            case REMOVE -> effectREMOVE(message);
        }
    }


    public boolean contains(String element){
        return this.store.containsKey(element);
    }
}