package scatterchat.protocol.signal;

public record ChatMessageSignal(String client, String topic, String message) implements Signal {}