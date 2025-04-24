package scatterchat.protocol.message.dht;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


public record DHTRep(List<String> ips) {

    public DHTRep(JSONObject json) {
        this(convertJSONArrayToList(json.getJSONArray("ips")));
    }

    private static List<String> convertJSONArrayToList(JSONArray jsonArray) {
        List<String> result = new ArrayList<>();
        jsonArray.forEach(x -> result.add((String) x));
        return result;
    }
}