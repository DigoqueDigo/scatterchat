package scatterchat.protocol.message.chat;


public record ChatServerEntry(String repAddress, String pullAddress, String interPubAddress, String extPubAddress) {

    public ChatServerEntry(String repAddress) {
        this(
            incrementPort(repAddress, 0),
            incrementPort(repAddress, 1),
            incrementPort(repAddress, 2),
            incrementPort(repAddress, 3)
        );
    }

    private static String incrementPort(String baseAddress, int increment) {
        String[] parts = baseAddress.split(":");
        int basePort = Integer.parseInt(parts[2]);
        return parts[0] + parts[1] + (basePort + increment);

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
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.repAddress);
        buffer.append(", ").append(this.pullAddress);
        buffer.append(", ").append(this.interPubAddress);
        buffer.append(", ").append(this.extPubAddress);
        return buffer.toString();
    }
}
