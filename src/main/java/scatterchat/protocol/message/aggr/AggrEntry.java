package scatterchat.protocol.message.aggr;

import java.util.Comparator;


public record AggrEntry(String scRepAddress, int totalTopics, int totalClients) {

    public static final Comparator<AggrEntry> CompareByTopicsClientsName = Comparator
        .comparingInt(AggrEntry::totalTopics)
        .thenComparingInt(AggrEntry::totalClients)
        .thenComparing(AggrEntry::scRepAddress);

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
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(scRepAddress);
        buffer.append(", ").append(totalClients);
        buffer.append(", ").append(totalTopics);
        return buffer.toString();
    }
}