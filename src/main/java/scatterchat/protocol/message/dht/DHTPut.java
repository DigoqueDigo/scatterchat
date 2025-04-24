package scatterchat.protocol.message.dht;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


public record DHTPut(int code, String room, List<String> ips) implements IJson {

    public DHTPut {
        if (code != 1) {
            throw new IllegalArgumentException("Only code 2 is allowed for DHTPut");
        }
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject()
            .put("code", code)
            .put("room", room)
            .put("ips", new JSONArray(ips));
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(code);
        buffer.append(", ").append(room);
        buffer.append(", ").append(ips);
        return buffer.toString();
    }
}
