package scatterchat.crdt;

import java.util.Set;


public record ORSetAction(String element, OrSetEntry clock, Operation operation, Set<OrSetEntry> entries) {

    public enum Operation {
        ADD,
        REMOVE,
    }
}
