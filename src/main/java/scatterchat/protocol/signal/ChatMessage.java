package scatterchat.protocol.signal;

public record ChatMessage(String client, String topic, String message) implements Signal {}