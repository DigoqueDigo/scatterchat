package scatterchat.protocol.message.chat;

import java.util.Comparator;


public record ChatServerEntry(String repAddress, String pullAddress, String interPubAddress, String extPubAddress, String loggerAddress, int loggerPort) implements Comparable<ChatServerEntry> {

    public ChatServerEntry(String repAddress) {
        this(
            getZMQAddress(repAddress, 0),
            getZMQAddress(repAddress, 1),
            getZMQAddress(repAddress, 2),
            getZMQAddress(repAddress, 3),
            getBaseAddress(repAddress),
            getBasePort(repAddress) + 4
        );
    }

    private static String getBaseAddress(String baseAddress) {
        return baseAddress.split(":")[1].replace("//", "");
    }

    private static int getBasePort(String baseAddress) {
        return Integer.parseInt(baseAddress.split(":")[2]);
    }

    private static String getZMQAddress(String baseAddress, int increment) {
        String[] parts = baseAddress.split(":");
        int basePort = Integer.parseInt(parts[2]);
        return String.join(":", parts[0], parts[1], String.valueOf(basePort + increment));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatServerEntry other)) return false;
        return repAddress.equals(other.repAddress);
    }

    @Override
    public int hashCode() {
        return repAddress.hashCode();
    }

    @Override
    public int compareTo(ChatServerEntry other) {
        return Comparator
            .comparing(ChatServerEntry::repAddress)
            .compare(this, other);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.repAddress);
        // buffer.append(", ").append(this.pullAddress);
        // buffer.append(", ").append(this.interPubAddress);
        // buffer.append(", ").append(this.extPubAddress);
        // buffer.append(", ").append(this.loggerAddress);
        // buffer.append(", ").append(this.loggerPort);
        return buffer.toString();
    }
}