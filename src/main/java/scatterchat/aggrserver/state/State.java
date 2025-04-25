package scatterchat.aggrserver.state;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.sarojaba.prettytable4j.PrettyTable;

import scatterchat.protocol.message.cyclon.CyclonEntry;


public class State{

    public static final int CYCLON_CAPACITY = 3;
    public static final int CYCLON_SHUFFLE_LENGTH = 2;

    private boolean cyclonOnGoing;
    private CyclonEntry myCyclonEntry;

    private List<CyclonEntry> nodesSent;
    private List<CyclonEntry> neighbours;

    public State(JSONObject config) {
        this.cyclonOnGoing = false;
        this.nodesSent = new ArrayList<>();
        this.neighbours = new ArrayList<>();
        this.myCyclonEntry = new CyclonEntry(config.getString("tcpExtPull"));
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
        PrettyTable ptNodesSent = PrettyTable.fieldNames("Node Sent Pull Address");
        PrettyTable ptNeighbours = PrettyTable.fieldNames("Neighbour Pull Address");

        this.nodesSent.forEach(x -> ptNodesSent.addRow(x.pullAddress()));
        this.neighbours.forEach(x -> ptNeighbours.addRow(x.pullAddress()));

        buffer.append(this.cyclonOnGoing);
        buffer.append(", ").append(this.myCyclonEntry);
        buffer.append("\n").append(ptNeighbours.toString());
        buffer.append("\n").append(ptNodesSent.toString());

        return buffer.toString();
    }
}