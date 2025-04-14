package scatterchat.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.rxjava3.core.Flowable;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.LogMessage;
import scatterchat.Rx3LogServiceGrpc;
import scatterchat.Rx3LogServiceGrpc.RxLogServiceStub;


public class Client {

    public static void main(String[] args) throws Exception{

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        RxLogServiceStub logStub = Rx3LogServiceGrpc.newRxStub(channel);

        logStub.getLogs(Flowable.just("hello",  "world").map(m -> LogMessage.newBuilder().setMessage(m).build()))
            .subscribe(
                log -> {
                    System.out.println("[gRPC LOG] " + log.getMessage());
                    channel.shutdownNow();
                },
                Throwable::printStackTrace,
                () -> System.out.println("[gRPC LOG] Stream closed")
            );

        ZContext context = new ZContext(1);
        ZMQ.Socket socket = context.createSocket(SocketType.REQ);
        socket.connect("tcp://localhost:5555");


        String request = "Hello via ZeroMQ!";
        System.out.println("[ZeroMQ] Sending: " + request);

        socket.send(request);

        byte[] reply = socket.recv();
        System.out.println("[ZeroMQ] Received: " + new String(reply));

        socket.close();
        context.close();
    }
}