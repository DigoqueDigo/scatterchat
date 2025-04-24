package scatterchat.protocol.message.aggr;

import java.util.Comparator;

import scatterchat.protocol.message.chat.ChatServerEntry;


public record AggrEntry(ChatServerEntry chatServerEntry, int totalTopics, int totalClients) implements Comparable<AggrEntry> {

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AggrEntry other)) return false;
        return chatServerEntry != null && chatServerEntry.equals(other.chatServerEntry);
    }

    @Override
    public int hashCode() {
        return chatServerEntry != null ? chatServerEntry.hashCode() : 0;
    }

    @Override
    public int compareTo(AggrEntry other) {
        return Comparator
            .comparingInt(AggrEntry::totalTopics)
            .thenComparingInt(AggrEntry::totalClients)
            .thenComparing(AggrEntry::chatServerEntry)
            .compare(this, other);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(chatServerEntry);
        buffer.append(", ").append(totalTopics);
        buffer.append(", ").append(totalClients);
        return buffer.toString();
    }
}