package scatterchat.chatserver.log;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import scatterchat.LogMessageReply;
import scatterchat.LogMessageRequest;
import scatterchat.Rx3LogServiceGrpc;


public class LogServer extends Rx3LogServiceGrpc.LogServiceImplBase implements Runnable{

    private int logServerPort;
    private String logServerAddress;


    public LogServer(String logServerAddress, int logServerPort){
        this.logServerAddress = logServerAddress;
        this.logServerPort = logServerPort;
    }

    // TODO :: ADAPTAR PARA SACAR OS LOGS DO STATE

    @Override
    public Flowable<LogMessageReply> getLogs(Single<LogMessageRequest> request){
        request.subscribe(m -> System.out.println("[GRPC] received: " + m.getHistory()));
        return Flowable.range(1, 1000)
        .doOnNext(m -> System.out.println("[GRPC] send: "  + m))
        .map(n -> LogMessageReply.newBuilder().setMessage(String.valueOf(n)).build());
    }


    @Override
    public void run(){

        try{
            Server logServer = ServerBuilder
                    .forPort(50051)
                    .addService(new LogServer(logServerAddress, logServerPort))
                    .build()
                    .start();

            System.out.println("[gRPC] LogServer started on: " + logServerAddress + ":" + logServerPort);
            logServer.awaitTermination();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}