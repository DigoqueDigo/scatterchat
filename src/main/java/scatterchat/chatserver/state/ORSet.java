package scatterchat.chatserver.state;

import scatterchat.crdt.CRDTEntry;
import scatterchat.protocol.messages.crtd.ORSetMessage;
import scatterchat.protocol.messages.crtd.ORSetMessage.Operation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ORSet {

    private CRDTEntry clock;
    private final Map<String, Set<CRDTEntry>> store;

    public ORSet(String nodeId) {
        this.store = new HashMap<>();
        this.clock = new CRDTEntry(nodeId, 0);
    }

    private ORSetMessage prepareAdd(Operation operation, String element) {
        this.clock = this.clock.increment();
        this.store.putIfAbsent(element, new HashSet<>());
        Set<CRDTEntry> entries = new HashSet<>(this.store.get(element));
        return new ORSetMessage(element, clock, operation, entries);
    }

    private ORSetMessage prepareRemove(Operation operation, String element) {
        this.store.putIfAbsent(element, new HashSet<>());
        Set<CRDTEntry> entries = new HashSet<>(this.store.get(element));
        return new ORSetMessage(element, null, operation, entries);
    }

    private void effectAdd(ORSetMessage message) {
        String element = message.element();
        this.store.putIfAbsent(element, new HashSet<>());

        Set<CRDTEntry> entries = this.store.get(element);
        entries.removeAll(message.entries());
        entries.add(message.clock());
    }

    private void effectRemove(ORSetMessage message) {
        String element = message.element();
        Set<CRDTEntry> entries = this.store.get(element);
        entries.removeAll(message.entries());

        if (entries.isEmpty()) {
            this.store.remove(element);
        }
    }

    public ORSetMessage prepare(Operation operation, String element) {
        return switch (operation) {
            case ADD -> prepareAdd(operation, element);
            case REMOVE -> prepareRemove(operation, element);
        };
    }

    public void effect(ORSetMessage message) {
        switch (message.operation()) {
            case ADD -> effectAdd(message);
            case REMOVE -> effectRemove(message);
        }
    }

    public boolean contains(String element) {
        return this.store.containsKey(element);
    }
}