package scatterchat.crdt;


public record OrSetEntry(String id, int logicClock) {

    public OrSetEntry increment() {
        return new OrSetEntry(this.id, this.logicClock + 1);
    }
}