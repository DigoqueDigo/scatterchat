package scatterchat.protocol.signal;

public record TopicExitSignal(String client, String topic) implements Signal {}
