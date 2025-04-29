package scatterchat.protocol.signal;

public record LogRequestSignal(int history) implements Signal {}