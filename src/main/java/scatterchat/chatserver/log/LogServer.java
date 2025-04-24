package scatterchat.chatserver.log;

import org.json.JSONObject;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import scatterchat.LogMessageReply;
import scatterchat.LogMessageRequest;
import scatterchat.Rx3LogServiceGrpc;


public class LogServer extends Rx3LogServiceGrpc.LogServiceImplBase implements Runnable {

    private JSONObject config;


    public LogServer(JSONObject config) {
        this.config = config;
    }

    @Override
    public Flowable<LogMessageReply> getLogs(Single<LogMessageRequest> request){
        request.subscribe(m -> System.out.println("[GRPC] received: " + m.getHistory()));
        return Flowable.range(1, 1000)
        .doOnNext(m -> System.out.println("[GRPC] send: "  + m))
        .map(n -> LogMessageReply.newBuilder().setMessage(String.valueOf(n)).build());
    }

    @Override
    public void run() {

        try {
            int logPort = config.getInt("logPort");
            System.out.println("[LogServer] started");
            System.out.println("[LogServer] bind port: " + logPort);

            Server logServer = ServerBuilder
                .forPort(logPort)
                .addService(new LogServer(config))
                .build()
                .start();

            logServer.awaitTermination();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}