package scatterchat.crdt;

import scatterchat.crdt.ORSetAction.Operation;
import scatterchat.protocol.message.chat.ChatServerEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sarojaba.prettytable4j.PrettyTable;


public class ORSet {

    private ORSetEntry clock;
    private Map<String, Set<ORSetEntry>> store;

    public ORSet(ChatServerEntry nodeId) {
        this.store = new HashMap<>();
        this.clock = new ORSetEntry(nodeId, 0);
    }

    private ORSetAction prepareAdd(Operation operation, String element) {
        this.clock = this.clock.increment();
        this.store.putIfAbsent(element, new HashSet<>());
        Set<ORSetEntry> entries = new HashSet<>(this.store.get(element));
        return new ORSetAction(element, clock, operation, entries);
    }

    private ORSetAction prepareRemove(Operation operation, String element) {
        this.store.putIfAbsent(element, new HashSet<>());
        Set<ORSetEntry> entries = new HashSet<>(this.store.get(element));
        return new ORSetAction(element, null, operation, entries);
    }

    private void effectAdd(ORSetAction message) {
        String element = message.element();
        this.store.putIfAbsent(element, new HashSet<>());

        Set<ORSetEntry> entries = this.store.get(element);
        entries.removeAll(message.entries());
        entries.add(message.clock());
    }

    private void effectRemove(ORSetAction message) {
        String element = message.element();
        Set<ORSetEntry> entries = this.store.get(element);
        entries.removeAll(message.entries());

        if (entries.isEmpty()) {
            this.store.remove(element);
        }
    }

    public ORSetAction prepare(Operation operation, String element) {
        return switch (operation) {
            case ADD -> prepareAdd(operation, element);
            case REMOVE -> prepareRemove(operation, element);
        };
    }

    public void effect(ORSetAction message) {
        switch (message.operation()) {
            case ADD -> effectAdd(message);
            case REMOVE -> effectRemove(message);
        }
    }

    public boolean contains(String element) {
        return this.store.containsKey(element);
    }

    public Set<String> elements() {
        return new HashSet<>(this.store.keySet());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        PrettyTable ptStore = PrettyTable.fieldNames("Element", "Entries");

        this.store.forEach((element, entries) -> {
            entries.forEach(entry -> ptStore.addRow(element, entry));
        });

        buffer.append(this.clock);
        buffer.append("\n").append(ptStore.toString());
        return buffer.toString();
    }
}