package scatterchat.crdt;


public class CRDTEntry{

    private String id;
    private int logicClock;


    public CRDTEntry(String id, int logicClock){
        this.id = id;
        this.logicClock =logicClock;
    }


    private CRDTEntry(CRDTEntry CRDTEntry){
        this.id = CRDTEntry.id;
        this.logicClock = CRDTEntry.logicClock;
    }


    public CRDTEntry clone(){
        return new CRDTEntry(this);
    }


    public String getId(){
        return this.id;
    }


    public int getLogicClock(){
        return this.logicClock;
    }


    public void increment(){
        this.logicClock++;
    }


    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        CRDTEntry other = (CRDTEntry) obj;
        return logicClock == other.logicClock && id == other.id;
    }   
}