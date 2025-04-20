package scatterchat.protocol.message.cyclon;


public record CyclonEntry(String identity, String address) {

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
}