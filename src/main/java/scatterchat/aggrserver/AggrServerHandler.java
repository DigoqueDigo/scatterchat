package scatterchat.aggrserver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.aggrserver.state.State;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.aggr.Aggr;
import scatterchat.protocol.message.aggr.AggrEntry;
import scatterchat.protocol.message.aggr.AggrRep;
import scatterchat.protocol.message.aggr.AggrReq;
import scatterchat.protocol.message.cyclon.CyclonMessage;
import scatterchat.protocol.message.cyclon.CyclonOk;
import scatterchat.protocol.message.info.ServerStateRequest;
import scatterchat.protocol.message.info.ServerStateResponse;
import scatterchat.protocol.message.cyclon.CyclonEntry;
import scatterchat.protocol.message.cyclon.CyclonError;
import scatterchat.protocol.carrier.ZMQCarrier;


public class AggrServerHandler implements Runnable{

    private static final int C = 3;
    private static final int T = 5;

    private State state;
    private JSONObject config;

    private BlockingQueue<Message> received;
    private BlockingQueue<AggrRep> responded;
    private BlockingQueue<Message> outBuffer;

    private Set<String> startedAggrs;
    private Map<String, Integer> repeatedRounds;
    private Map<String, List<AggrEntry>> bestEntries;


    public AggrServerHandler(JSONObject config, State state, BlockingQueue<Message> received, BlockingQueue<AggrRep> responded, BlockingQueue<Message> outBuffer) {
        this.state = state;
        this.config = config;
        this.received = received;
        this.responded = responded;
        this.outBuffer = outBuffer;
        this.startedAggrs = new HashSet<>();
        this.bestEntries = new HashMap<>();
        this.repeatedRounds = new HashMap<>();
    }


    private List<CyclonEntry> mergeCyclonSubsets(List<CyclonEntry> neighbours, List<CyclonEntry> subset, List<CyclonEntry> excludeNodes) {

        Set<CyclonEntry> newNeighbours = new HashSet<>();
        CyclonEntry myCyclonEntry = this.state.getMyCyclonEntry();

        newNeighbours.addAll(neighbours);
        newNeighbours.addAll(subset);
        newNeighbours.remove(myCyclonEntry);

        while (newNeighbours.size() > State.CYCLON_CAPACITY && !excludeNodes.isEmpty()){
            CyclonEntry nodeSent = excludeNodes.remove(0);
            newNeighbours.remove(nodeSent);
        }

        return newNeighbours.stream()
            .limit(State.CYCLON_CAPACITY)
            .collect(Collectors.toList());
    }


    private void handleCyclonMessage(CyclonMessage message) throws InterruptedException{

        synchronized (this.state) {

            System.out.println("Handling Cyclon");
            System.out.println(state);

            CyclonEntry sender = this.state.getMyCyclonEntry();

            if (state.getCyclonOnGoing()){
                CyclonError cyclonError = new CyclonError(sender.pullAddress(), message.getSender());
                this.outBuffer.put(cyclonError);
            }

            else {
                List<CyclonEntry> neighbours = state.getNeighbours();
                Collections.shuffle(neighbours);

                int subSetLength = Math.min(neighbours.size(), State.CYCLON_SHUFFLE_LENGTH);
                List<CyclonEntry> subSet = neighbours.subList(0, subSetLength);

                CyclonOk cyclonOk = new CyclonOk(
                    sender.pullAddress(),
                    message.getSender(),
                    subSet
                );

                System.out.println("[SA AggrServerHandler] sent: " + cyclonOk);

                this.outBuffer.put(cyclonOk);
                state.setNeighbours(mergeCyclonSubsets(
                    neighbours, message.getSubSet(), subSet));

                System.out.println(state);
            }
        }
    }


    private void handleCyclonOk(CyclonOk message) {

        synchronized (this.state) {

            System.out.println("Handling CyclonOK");
            List<CyclonEntry> excludeNodes = this.state.getNodesSent();
            excludeNodes.add(0, new CyclonEntry(message.getSender()));

            List<CyclonEntry> newNeighbours = mergeCyclonSubsets(
                this.state.getNeighbours(),
                message.getSubSet(),
                excludeNodes
            );

            this.state.setCyclonOnGoing(false);
            this.state.setNeighbours(newNeighbours);
            System.out.println(state);
        }
    }


    private void handleCyclonError(CyclonError message) {
        synchronized (this.state) {
            System.out.println("Handling Cyclon ERROR");
            state.setCyclonOnGoing(false);
        }
    }


    private void handleAggrReq(AggrReq message, ZMQCarrier carrier) throws InterruptedException{

        synchronized (this.state) {

            String topic = message.getTopic();
            AggrEntry scState = getChatServerState(carrier);
            CyclonEntry sender = this.state.getMyCyclonEntry();

            this.startedAggrs.add(topic);
            this.bestEntries.put(topic, Arrays.asList(scState));

            List<AggrEntry> entries = this.bestEntries.get(topic);

            for (CyclonEntry neighbour : state.getNeighbours()) {
                Aggr aggrMessage = new Aggr(sender.pullAddress(), neighbour.pullAddress(), topic, entries);
                this.outBuffer.put(aggrMessage);
            }
        }
    }


    private void handleAggr(Aggr message, ZMQCarrier carrier) throws InterruptedException{

        synchronized (this.state) {

            String topic = message.getTopic();
            CyclonEntry sender = this.state.getMyCyclonEntry();

            this.repeatedRounds.putIfAbsent(topic, 0);
            int repeatedRounds = this.repeatedRounds.get(topic);

            if (!this.bestEntries.containsKey(topic)) {
                AggrEntry scState = getChatServerState(carrier);
                this.bestEntries.put(topic, Arrays.asList(scState));
            }

            List<AggrEntry> receivedEntries = message.getEntries();
            List<AggrEntry> oldEntries = this.bestEntries.get(topic);

            List<AggrEntry> newEntries = Stream.concat(
                receivedEntries.stream(),
                oldEntries.stream())
                    .distinct()
                    .sorted(AggrEntry.CompareByTopicsClientsName)
                    .limit(AggrServerHandler.C)
                    .collect(Collectors.toList());

            if (oldEntries.equals(newEntries) && newEntries.size() == AggrServerHandler.C){
                repeatedRounds += 1;
            } else {
                repeatedRounds = 0;
                this.bestEntries.put(topic, newEntries);
            }

            boolean starter = this.startedAggrs.contains(topic);
            int currentT = (starter) ? AggrServerHandler.T : AggrServerHandler.T + AggrServerHandler.T / 2; 

            if (repeatedRounds < currentT) {
                for (CyclonEntry neighbour : state.getNeighbours()) {
                    Aggr aggrMessage = new Aggr(sender.pullAddress(), neighbour.pullAddress(), topic, newEntries);                
                    this.outBuffer.put(aggrMessage);
                }
            }

            else if (starter && repeatedRounds == currentT) {
                AggrRep aggrRep = new AggrRep(sender.pullAddress(), "client", topic, true);
                this.responded.put(aggrRep);
            }

            this.repeatedRounds.put(topic, repeatedRounds);
        }
    }


    private AggrEntry getChatServerState(ZMQCarrier carrier) {

        carrier.sendMessage(new ServerStateRequest());
        ServerStateResponse serverStateResponse = (ServerStateResponse) carrier.receiveMessage();

        String scRepAddress = serverStateResponse.getSender();
        Map<String, Set<String>> serveState = serverStateResponse.getServerState();

        return new AggrEntry(
            scRepAddress,
            serveState.size(),
            serveState.values().stream().mapToInt(Collection::size).sum()
        );
    }


    @Override
    public void run() {

        try {

            ZContext context = new ZContext();
            ZMQ.Socket socket = context.createSocket(SocketType.REQ);
            ZMQCarrier carrier = new ZMQCarrier(socket);

            String connectionAddress = this.config
                .getJSONObject("sc")
                .getString("tcpExtRep");

            socket.connect(connectionAddress);
            System.out.println("[AggrServerHandler] started");
            System.out.println("[AggrServerHandler] connected: " + connectionAddress);

            Message message = null;;

            while ((message = this.received.take()) != null) {

                System.out.println("[AggrServerHandler] received: " + message);

                switch (message) {
                    case CyclonOk m -> handleCyclonOk(m);
                    case CyclonError m -> handleCyclonError(m);
                    case CyclonMessage m -> handleCyclonMessage(m);
                    case AggrReq m -> handleAggrReq(m, carrier);
                    case Aggr m -> handleAggr(m, carrier);
                    default -> System.out.println("[AggrServerHandler] unknown: " + message);
                }
            }

            socket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}