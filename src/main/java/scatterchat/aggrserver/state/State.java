package scatterchat.aggrserver.state;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import scatterchat.protocol.message.cyclon.CyclonEntry;
import scatterchat.utils.PrettyPrint;


public class State{

    public static final Integer CyclonCapacity = 8;
    public static final Integer CyclonShuffleLength = 4;

    private boolean cyclonOnGoing;
    private CyclonEntry myCyclonEntry;

    private List<CyclonEntry> nodesSent;
    private List<CyclonEntry> neighbours;

    public State(JSONObject config) {
        this.cyclonOnGoing = false;
        this.nodesSent = new ArrayList<>();
        this.neighbours = new ArrayList<>();
        this.myCyclonEntry = new CyclonEntry(
            config.getString("identity"),
            config.getString("tcpExtPub"),
            config.getString("tcpExtPubTimer"));
    }

    public boolean getCyclonOnGoing() {
        return this.cyclonOnGoing;
    }

    public CyclonEntry getMyCyclonEntry() {
        return this.myCyclonEntry;
    }

    public List<CyclonEntry> getNeighbours() {
        return new ArrayList<>(this.neighbours);
    }

    public List<CyclonEntry> getNodesSent() {
        return new ArrayList<>(this.nodesSent);
    }

    public void setCyclonOnGoing(boolean cyclonOnGoing) {
        this.cyclonOnGoing = cyclonOnGoing;
    }

    public void setNeighbours(List<CyclonEntry> neighbours) {
        this.neighbours = neighbours;
    }

    public void setNodesSent(List<CyclonEntry> nodesSent) {
        this.nodesSent = nodesSent;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("CyclonOnGoing: " + this.cyclonOnGoing);
        buffer.append("\n" + "MyCyclonEntry: " + this.myCyclonEntry);
        buffer.append("\n" + PrettyPrint.CyclonEntriestoString(this.neighbours, "Identity", "Neighbour Pub", "Neighbour PubTimer"));
        buffer.append("\n" + PrettyPrint.CyclonEntriestoString(this.nodesSent, "Identity", "Node Sent Pub", "Neighbour PubTImer"));
        return buffer.toString();
    }
}