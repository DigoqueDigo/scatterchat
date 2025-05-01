package scatterchat.protocol.signal;

public record UserLogSignal(String client, String topic, int lines) implements Signal {}