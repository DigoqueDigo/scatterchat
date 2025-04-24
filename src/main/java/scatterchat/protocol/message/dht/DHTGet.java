package scatterchat.protocol.message.dht;

import org.json.JSONObject;


public record DHTGet(int code, String room) implements IJson {

    public DHTGet {
        if (code != 0) {
            throw new IllegalArgumentException("Only code 2 is allowed for DHTPut");
        }
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject()
            .put("code", code)
            .put("room", room);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(code);
        buffer.append(", ").append(room);
        return buffer.toString();
    }
}