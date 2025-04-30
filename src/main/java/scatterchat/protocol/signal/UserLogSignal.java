package scatterchat.protocol.signal;

public record UserLogSignal(String client, String topic) implements Signal {}