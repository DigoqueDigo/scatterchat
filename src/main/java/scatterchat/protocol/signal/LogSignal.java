package scatterchat.protocol.signal;

public record LogSignal(String topic, int history) implements Signal {}