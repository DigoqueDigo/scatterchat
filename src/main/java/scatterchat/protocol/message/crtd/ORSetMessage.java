package scatterchat.protocol.message.crtd;

import scatterchat.crdt.CRDTEntry;

import java.util.Set;


public record ORSetMessage(String element, CRDTEntry clock, Operation operation, Set<CRDTEntry> entries) {

    public enum Operation {
        ADD,
        REMOVE,
    }
}
