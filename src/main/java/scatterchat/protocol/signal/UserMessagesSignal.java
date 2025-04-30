package scatterchat.protocol.signal;

public record UserMessagesSignal(String client, String topic) implements Signal {}