package scatterchat.protocol.messages.crtd;
import java.util.Set;
import scatterchat.crdt.CRDTEntry;


public class ORSetMessage{

    public enum OPERATION{
        ADD,
        REMOVE,
    };

    private String element;
    private CRDTEntry clock;
    private OPERATION operation;
    private Set<CRDTEntry> entries;


    public ORSetMessage(OPERATION operation, String element, Set<CRDTEntry> entries){
        this.operation = operation;
        this.element = element;
        this.clock = null;
        this.entries = entries;
    }


    public ORSetMessage(OPERATION operation, String element, CRDTEntry clock, Set<CRDTEntry> entries){
        this.operation = operation;
        this.element = element;
        this.clock = clock;
        this.entries = entries;
    }


    public OPERATION getOperation(){
        return this.operation;
    }


    public String getElement(){
        return this.element;
    }


    public CRDTEntry getClock(){
        return this.clock;
    }


    public Set<CRDTEntry> getEntries(){
        return this.entries;
    }
}
