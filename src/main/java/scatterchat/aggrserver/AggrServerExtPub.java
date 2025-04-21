package scatterchat.aggrserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

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


public class AggrServerExtPub implements Runnable{

    private static final int C = 3;
    private static final int T = 5;

    private State state;
    private JSONObject config;
    private BlockingQueue<Message> received;
    private BlockingQueue<AggrRep> response;

    private String startedcurrentAggrTopic;
    private Map<String, Integer> repeatedRoundsPerTopic;
    private Map<String, List<AggrEntry>> bestEntriesPerTopic;


    public AggrServerExtPub(JSONObject config, State state, BlockingQueue<Message> received, BlockingQueue<AggrRep> response) {
        this.state = state;
        this.config = config;
        this.received = received;
        this.response = response;
        this.startedcurrentAggrTopic = null;
        this.bestEntriesPerTopic = new HashMap<>();
        this.repeatedRoundsPerTopic = new HashMap<>();
    }


    private List<CyclonEntry> mergeCyclonSubset(List<CyclonEntry> neighbours, List<CyclonEntry> subset, List<CyclonEntry> nodesSent) {

        Set<CyclonEntry> newNeighbours = new HashSet<>();
        newNeighbours.addAll(neighbours);
        newNeighbours.addAll(subset);
        newNeighbours.remove(state.getMyCyclonEntry());

        while (newNeighbours.size() > State.CyclonCapacity && !nodesSent.isEmpty()){
            CyclonEntry nodeSent = nodesSent.remove(0);
            newNeighbours.remove(nodeSent);
        }

        return new ArrayList<>(newNeighbours);
    }


    private void handleCyclonMessage(CyclonMessage message, ZMQCarrier carrier) {

        synchronized (state) {

            System.out.println(state);
            state.setCyclonOnGoing(false);
            
            if (state.getCyclonOnGoing()){
                CyclonError cyclonError = new CyclonError();
                carrier.sendMessage(cyclonError);
            }

            else {
                String senderIdentity = message.getSenderIdentity();
                List<CyclonEntry> neighbours = state.getNeighbours();
                int subSetLength = Math.min(neighbours.size(), State.CyclonShuffleLength);

                Collections.shuffle(neighbours);

                List<CyclonEntry> subSet = neighbours.subList(0, subSetLength);
                CyclonOk cyclonOk = new CyclonOk(new HashSet<>(subSet));

                System.out.println("SENDERIDENTITY: " + senderIdentity);
                System.out.println("[SA AggrServerExtPub] Sent: " + cyclonOk);

                carrier.sendMessageWithTopic(senderIdentity, cyclonOk);
                state.setNeighbours(mergeCyclonSubset(neighbours, message.getSubSet(), subSet));
                System.out.println(state);

                // TODO :: INFORMAR OS SUB DOS NOVOS VIZINHOS
            }
        }
    }


    private void handleCyclonOk(CyclonOk message) {
        synchronized (state) {
            state.setCyclonOnGoing(false);
            this.state.setNeighbours(
                mergeCyclonSubset(
                    state.getNeighbours(),
                    message.getSubSet(),
                    state.getNodesSent()));

            System.out.println(state);

            // TODO :: INFORMAR OS SUB DOS NOVOS VIZINHOS
        }
    }


    private void handleCyclonError(CyclonError message) {
        synchronized (state) {
            state.setCyclonOnGoing(false);
        }
    }


    private void handleAggrReq(AggrReq message, ZMQCarrier routerCarrier, ZMQCarrier reqCarrier) {

        synchronized (state) {

            String topic = message.getTopic();
            AggrEntry scState = getChatServerState(reqCarrier);

            this.bestEntriesPerTopic.put(topic, new ArrayList<>());
            this.bestEntriesPerTopic.get(topic).add(scState);
            this.startedcurrentAggrTopic = topic;

            Aggr aggr = new Aggr(topic, this.bestEntriesPerTopic.get(topic));

            for (CyclonEntry neighbour : state.getNeighbours()) {
                routerCarrier.sendMessageWithTopic(neighbour.identity(), aggr);
            }
        }
    }


    private void handleAggr(Aggr message, ZMQCarrier routerCarrier, ZMQCarrier reqCarrier) throws InterruptedException{

        synchronized (state) {

            String topic = message.getTopic();
            this.repeatedRoundsPerTopic.putIfAbsent(topic, 0);

            if (this.bestEntriesPerTopic.get(topic) == null) {
                AggrEntry scState = getChatServerState(reqCarrier);
                this.bestEntriesPerTopic.put(topic, Arrays.asList(scState));
            }

            int repeatedRounds = this.repeatedRoundsPerTopic.get(topic);
            List<AggrEntry> receivedEntries = message.getEntries();
            List<AggrEntry> oldEntries = this.bestEntriesPerTopic.get(topic);

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
                this.bestEntriesPerTopic.put(topic, newEntries);
            }

            boolean isMyAggregation = topic.equals(this.startedcurrentAggrTopic);
            int myT = (isMyAggregation) ? T : T + T / 2; 

            if (repeatedRounds < myT) {
                Aggr aggr = new Aggr(topic, newEntries);                
                for (CyclonEntry neighbour : state.getNeighbours()) {
                    routerCarrier.sendMessageWithTopic(neighbour.identity(), aggr);
                }
            }

            else if (isMyAggregation && repeatedRounds == myT) {
                AggrRep response = new AggrRep(topic, true);
                this.response.put(response);
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

        try {

            ZContext context = new ZContext();
            ZMQ.Socket pubSocket = context.createSocket(SocketType.SUB);
            ZMQ.Socket reqSocket = context.createSocket(SocketType.REQ);

            ZMQCarrier pubCarrier = new ZMQCarrier(pubSocket);
            ZMQCarrier reqCarrier = new ZMQCarrier(reqSocket);

            Message message = null;;

            pubSocket.bind(config.getString("tcpExtPub"));
            pubSocket.bind(config.getString("inprocPubSub"));

            System.out.println("[AggrServerExtPub] Bind: " + config.getString("tcpExtPub"));
            System.out.println("[AggrServerExtPub] Bind: " + config.getString("inprocPubSub"));

            while ((message = this.received.take()) != null) {

                System.out.println("[AggrServerExtPub] Received: " + message);

                switch (message) {
                    case CyclonOk m -> handleCyclonOk(m);
                    case CyclonError m -> handleCyclonError(m);
                    case CyclonMessage m -> handleCyclonMessage(m, pubCarrier);
                    case Aggr m -> handleAggr(m, pubCarrier, reqCarrier);
                    case AggrReq m -> handleAggrReq(m, pubCarrier, reqCarrier);
                    default -> System.out.println("[SA Propagator] Unknown: " + message);
                }
            }

            pubSocket.close();
            reqSocket.close();
            context.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}