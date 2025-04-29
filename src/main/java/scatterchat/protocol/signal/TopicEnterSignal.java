package scatterchat.protocol.signal;

public record TopicEnterSignal(String client, String topic) implements Signal {}
