package scatterchat.aggrserver.state;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import scatterchat.protocol.message.cyclon.CyclonEntry;


public class State{

    private static final Integer capacity = 8;
    private static final Integer shuffleLength = 4;

    private boolean cyclonOnGoing;
    private CyclonEntry cyclonEntry;

    private List<CyclonEntry> nodesSent;
    private List<CyclonEntry> neighbours;

    public State(JSONObject config) {
        this.cyclonOnGoing = false;
        this.nodesSent = new ArrayList<>();
        this.neighbours = new ArrayList<>();
        this.cyclonEntry = new CyclonEntry(
            config.getString("identity"),
            config.getString("tcpExtRouter"));
    }

    public int getCapacity() {
        return State.capacity;
    }

    public int getShuffleLength() {
        return State.shuffleLength;
    }

    public boolean getCyclonOnGoing() {
        return this.cyclonOnGoing;
    }

    public CyclonEntry getCyclonEntry() {
        return this.cyclonEntry;
    }

    public List<CyclonEntry> getNeighbours() {
        return this.neighbours;
    }

    public List<CyclonEntry> getNodesSent() {
        return this.nodesSent;
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
}