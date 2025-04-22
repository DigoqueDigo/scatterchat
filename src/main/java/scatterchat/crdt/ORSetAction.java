package scatterchat.crdt;

import java.util.Set;


public record ORSetAction(String element, ORSetEntry clock, Operation operation, Set<ORSetEntry> entries) {

    public enum Operation {
        ADD,
        REMOVE,
    }
}
