package scatterchat.protocol.message.aggr;

import java.util.Comparator;


public record AggrEntry(String scRepAddress, int totalTopics, int totalClients) implements Comparable<AggrEntry> {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AggrEntry other)) return false;
        return scRepAddress != null && scRepAddress.equals(other.scRepAddress);
    }

    @Override
    public int hashCode() {
        return scRepAddress != null ? scRepAddress.hashCode() : 0;
    }

    @Override
    public int compareTo(AggrEntry other) {
        return Comparator
            .comparingInt(AggrEntry::totalTopics)
            .thenComparingInt(AggrEntry::totalClients)
            .thenComparing(AggrEntry::scRepAddress)
            .compare(this, other);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(scRepAddress);
        buffer.append(", ").append(totalTopics);
        buffer.append(", ").append(totalClients);
        return buffer.toString();
    }
}