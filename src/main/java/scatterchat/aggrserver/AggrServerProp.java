package scatterchat.aggrserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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


public class AggrServerProp implements Runnable{

    private static final int C = 3;
    private static final int T = 5;

    private State state;
    private JSONObject config;

    private AggrReq clientRequest;
    private Map<String, Integer> repeatedRoundsPerTopic;
    private Map<String, List<AggrEntry>> entriesPerTopic;


    public AggrServerProp(JSONObject config, State state) {
        this.state = state;
        this.config = config;
        this.clientRequest = null;
        this.entriesPerTopic = new HashMap<>();
        this.repeatedRoundsPerTopic = new HashMap<>();
    }


    private void mergeCyclonSubset(List<CyclonEntry> neighbours, List<CyclonEntry> subset) {

        int capacity = state.getCapacity();
        List<CyclonEntry> nodesSent = state.getNodesSent();
        Set<CyclonEntry> tempNeighbours = new HashSet<>();

        tempNeighbours.addAll(neighbours);
        tempNeighbours.addAll(subset);
        tempNeighbours.remove(state.getCyclonEntry());

        while (tempNeighbours.size() > capacity && !nodesSent.isEmpty()){
            CyclonEntry nodeSent = nodesSent.remove(0);
            tempNeighbours.remove(nodeSent);
        }

        state.setNodesSent(null);
        state.setNeighbours(new ArrayList<>(tempNeighbours));
    }



    private void handleCyclonMessage(CyclonMessage message, ZMQCarrier carrier) {

        synchronized (state) {

            state.setCyclonOnGoing(false);

            if (state.getCyclonOnGoing()){
                CyclonError cyclonError = new CyclonError();
                carrier.sendMessage(cyclonError);
            }

            else {
                String senderIdentity = message.getSenderIdentity();
                List<CyclonEntry> neighbours = state.getNeighbours();
                int subSetLength = Math.min(neighbours.size(), state.getShuffleLength());

                Collections.shuffle(neighbours);

                List<CyclonEntry> subSet = neighbours.subList(0, subSetLength);
                CyclonOk cyclonOk = new CyclonOk(new HashSet<>(subSet));

                carrier.sendMessageWithIdentity(senderIdentity, cyclonOk);
                mergeCyclonSubset(neighbours, message.getSubSet());
            }
        }
    }


    private void handleCyclonOk(CyclonOk message) {
        synchronized (state) {
            state.setCyclonOnGoing(false);
            mergeCyclonSubset(state.getNeighbours(), message.getSubSet());
        }
    }


    private void handleCyclonError(CyclonError message) {
        synchronized (state) {
            state.setCyclonOnGoing(false);
        }
    }


    private void handleAggrReq(AggrReq message, ZMQCarrier routerCarrier, ZMQCarrier reqCarrier) {

        synchronized (state) {

            if (this.clientRequest != null) {
                AggrRep aggrRep = new AggrRep(config.getString("identity"), message.getTopic(), false);
                routerCarrier.sendMessageWithIdentity(message.getSenderIdentity(), aggrRep);
            }

            else {
                String topic = message.getTopic();
                AggrEntry scState = getChatServerState(reqCarrier);

                this.entriesPerTopic.put(topic, new ArrayList<>());
                this.entriesPerTopic.get(topic).add(scState);
                this.clientRequest = message;

                Aggr aggr = new Aggr(topic, this.entriesPerTopic.get(topic));

                for (CyclonEntry neighbour : state.getNeighbours()) {
                    routerCarrier.connect(neighbour.address());
                    routerCarrier.sendMessageWithIdentity(neighbour.identity(), aggr);
                }
            }
        }
    }


    private void handleAggr(Aggr message, ZMQCarrier routerCarrier, ZMQCarrier reqCarrier) {

        synchronized (state) {

            String topic = message.getTopic();
            this.repeatedRoundsPerTopic.putIfAbsent(topic, 0);
            this.entriesPerTopic.putIfAbsent(topic, new ArrayList<>());
            
            int repeatedRounds = this.repeatedRoundsPerTopic.get(topic);
            List<AggrEntry> receivedEntries = message.getEntries();
            List<AggrEntry> oldEntries = this.entriesPerTopic.get(topic);
            
            receivedEntries.addAll(oldEntries);
            List<AggrEntry> newEntries = new ArrayList<>(new HashSet<>(receivedEntries));
            newEntries.sort(AggrEntry.CompareByTopicsClientsName);

            if (newEntries.size() >= C){
                newEntries = newEntries.subList(0, C);
            }

            if (oldEntries.equals(newEntries) && newEntries.size() == C){
                repeatedRounds += 1;
            }

            else {
                repeatedRounds = 0;
                this.entriesPerTopic.put(topic, newEntries);
            }

            boolean isMyClient = this.clientRequest != null && this.clientRequest.getTopic().equals(topic);
            int myT = (isMyClient) ? T : T + T / 2; 

            if (repeatedRounds < myT) {
                Aggr aggr = new Aggr(topic, newEntries);                
                for (CyclonEntry neighbour : state.getNeighbours()) {
                    routerCarrier.connect(neighbour.address());
                    routerCarrier.sendMessageWithIdentity(neighbour.identity(), aggr);
                }
            }

            else if (isMyClient && repeatedRounds == myT) {
                String clientIdentity = this.clientRequest.getSenderIdentity();
                AggrRep response = new AggrRep(config.getString("identity"), topic, true);
                reqCarrier.sendMessageWithIdentity(clientIdentity, response);
                // TODO :: REGISTAR O TOPICO NA DHT
            }

            this.repeatedRoundsPerTopic.put(topic, repeatedRounds);
        }
    }


    private AggrEntry getChatServerState(ZMQCarrier carrier) {

        carrier.sendMessage(new ServerStateRequest());
        ServerStateResponse serverStateResponse = (ServerStateResponse) carrier.receiveMessage();

        String scAddress = serverStateResponse.getSender();
        Map<String, Set<String>> serveState = serverStateResponse.getServerState();

        return new AggrEntry(
            scAddress,
            serveState.size(),
            serveState.values().stream().mapToInt(Collection::size).sum());
    }


    @Override
    public void run() {

        ZContext context = new ZContext();

        ZMQ.Socket reqSocket = context.createSocket(SocketType.REQ);
        ZMQ.Socket routerSocket = context.createSocket(SocketType.ROUTER);

        ZMQCarrier reqCarrier = new ZMQCarrier(reqSocket);
        ZMQCarrier routerCarrier = new ZMQCarrier(routerSocket);

        reqSocket.connect(config.getString("tcpExtRep"));
        routerSocket.bind(config.getString("tcpExtRouter"));
        routerSocket.setIdentity(config.getString("identity").getBytes(ZMQ.CHARSET));

        Message message = null;

        while ((message = routerCarrier.receiveMessage()) != null) {

            System.out.println("[SA Propagator] Received: " + message);

            switch (message) {
                case CyclonOk m -> handleCyclonOk(m);
                case CyclonError m -> handleCyclonError(m);
                case CyclonMessage m -> handleCyclonMessage(m, routerCarrier);
                case Aggr m -> handleAggr(m , routerCarrier, reqCarrier);
                case AggrReq m -> handleAggrReq(m, routerCarrier, reqCarrier);
                default -> System.out.println("[SA Propagator] Unknown: " + message);
            }
        }

        routerSocket.close();
        context.close();
    }
}