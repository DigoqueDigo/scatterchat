package scatterchat.crdt;

import java.util.Set;


public record ORSetAction(String element, CRDTEntry clock, Operation operation, Set<CRDTEntry> entries) {

    public enum Operation {
        ADD,
        REMOVE,
    }
}
