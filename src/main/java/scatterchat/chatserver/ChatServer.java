package scatterchat.chatserver;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.reactivex.rxjava3.core.Flowable;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import scatterchat.LogMessage;
import scatterchat.Rx3LogServiceGrpc;
import java.io.IOException;


public class ChatServer extends Rx3LogServiceGrpc.LogServiceImplBase{

    public static void main(String[] args) throws IOException, InterruptedException{

        Server logServer = ServerBuilder
                .forPort(50051)
                .addService(new ChatServer())
                .build()
                .start();

        System.out.println("[gRPC] Server started on port 50051.");

        Thread chatSocket = Thread.ofVirtual().start(ChatServer::startZeroMQServer);

        logServer.awaitTermination();
        chatSocket.join();
    }


    private static void startZeroMQServer() {

        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(SocketType.REP);

        socket.bind("tcp://localhost:5555");
        System.out.println("[ZeroMQ] Server listening on tcp://localhost:5555");

        while (!Thread.currentThread().isInterrupted()) {

            byte[] request = socket.recv(0);
            String receivedMessage = new String(request, ZMQ.CHARSET);
            System.out.println("[ZeroMQ] Received: " + receivedMessage);

            String reply = "Echo: " + receivedMessage;
            socket.send(reply.getBytes(ZMQ.CHARSET), 0);
        }

        socket.close();
        context.close();
    }


    @Override
    public Flowable<LogMessage> getLogs(Flowable<LogMessage> request){
        return request
            .map(m -> m.getMessage())
            .doOnNext(m -> System.out.println("[GRPC] Received: "  + m))
            .map(n -> LogMessage.newBuilder().setMessage(n).build());
    }
}