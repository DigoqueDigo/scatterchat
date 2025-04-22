package scatterchat.protocol.message.cyclon;


public record CyclonEntry(String pullAddress) {

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof CyclonEntry other)) return false;
        return pullAddress.equals(other.pullAddress);
    }

    @Override
    public int hashCode() {
        return pullAddress.hashCode();
    }

    @Override
    public String toString() {
        return this.pullAddress;
    }
}