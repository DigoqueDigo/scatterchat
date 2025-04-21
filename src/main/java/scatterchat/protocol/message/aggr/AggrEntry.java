package scatterchat.protocol.message.aggr;

import java.util.Comparator;


public record AggrEntry(String sc, int totalTopics, int totalClients) {

    public static final Comparator<AggrEntry> CompareByTopicsClientsName = Comparator
        .comparingInt(AggrEntry::totalTopics)
        .thenComparingInt(AggrEntry::totalClients)
        .thenComparing(AggrEntry::sc);

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AggrEntry other)) return false;
        return sc != null && sc.equals(other.sc);
    }

    @Override
    public int hashCode() {
        return sc != null ? sc.hashCode() : 0;
    }
}