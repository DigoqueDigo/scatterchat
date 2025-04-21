package scatterchat.protocol.message.cyclon;


public record CyclonEntry(String identity, String pubAddress, String pubTimerAddress) {

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof CyclonEntry other)) return false;
        return identity.equals(other.identity);
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Identity: " + identity);
        buffer.append("\t pubAddress: " + pubAddress);
        buffer.append("\t pubTimerAddress: " + pubTimerAddress);
        return buffer.toString();
    }
}