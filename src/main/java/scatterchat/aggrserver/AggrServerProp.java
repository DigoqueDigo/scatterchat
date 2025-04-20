package scatterchat.aggrserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import scatterchat.aggrserver.state.State;
import scatterchat.protocol.message.Message;
import scatterchat.protocol.message.cyclon.CyclonMessage;
import scatterchat.protocol.message.cyclon.CyclonOk;
import scatterchat.protocol.message.cyclon.CyclonEntry;
import scatterchat.protocol.message.cyclon.CyclonError;
import scatterchat.protocol.carrier.ZMQCarrier;


public class AggrServerProp implements Runnable{

    private State state;
    private JSONObject config;


    public AggrServerProp(JSONObject config, State state) {
        this.state = state;
        this.config = config;
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

                String senderIdentity = message.getIdentity();
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


    @Override
    public void run() {

        ZContext context = new ZContext();
        ZMQ.Socket routerSocket = context.createSocket(SocketType.ROUTER);
        ZMQCarrier routerCarrier = new ZMQCarrier(routerSocket);

        routerSocket.bind(config.getString("extRouterTCPAddress"));
        routerSocket.bind(config.getString("interRouterProcAddress"));
        routerSocket.setIdentity(config.getString("identity").getBytes(ZMQ.CHARSET));

        Message message = null;

        while ((message = routerCarrier.receiveMessage()) != null) {

            System.out.println("[SA Propagator] Received: " + message);

            switch (message) {
                case CyclonOk m -> handleCyclonOk(m);
                case CyclonError m -> handleCyclonError(m);
                case CyclonMessage m -> handleCyclonMessage(m, routerCarrier);
                default -> System.out.println("[SA Propagator] Unknown: " + message);
            }
        }

        routerSocket.close();
        context.close();
    }
}