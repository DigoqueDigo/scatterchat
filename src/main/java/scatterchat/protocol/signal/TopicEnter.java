package scatterchat.protocol.signal;

public record TopicEnter(String client, String topic) implements Signal {}
