package scatterchat.protocol.signal;

public record UserMessagesRequest(String client, String topic) implements Signal {}