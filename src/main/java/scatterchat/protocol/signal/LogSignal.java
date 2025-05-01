package scatterchat.protocol.signal;

public record LogSignal(String topic, int lines) implements Signal {}