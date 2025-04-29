package scatterchat.protocol.signal;

public record TopicExit(String client, String topic) implements Signal {}
