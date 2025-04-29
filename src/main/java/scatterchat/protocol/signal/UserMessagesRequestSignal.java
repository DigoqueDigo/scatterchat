package scatterchat.protocol.signal;

public record UserMessagesRequestSignal(String client, String topic) implements Signal {}