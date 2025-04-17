package scatterchat.crdt;


public record CRDTEntry(String id, int logicClock) {

    public CRDTEntry increment() {
        return new CRDTEntry(this.id, this.logicClock + 1);
    }
}