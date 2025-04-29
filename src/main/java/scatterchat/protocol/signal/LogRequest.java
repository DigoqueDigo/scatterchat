package scatterchat.protocol.signal;

public record LogRequest(int history) implements Signal {}