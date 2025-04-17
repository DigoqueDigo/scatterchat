package scatterchat.chatserver.log;

import java.net.InetSocketAddress;
import org.json.JSONObject;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.Server;
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

            final int port = config.getInt("logServerPort");
            final String address = config.getString("logServerAddress");

            Server logServer = NettyServerBuilder
                .forAddress(new InetSocketAddress(address, port))
                .addService(new LogServer(config))
                .build()
                .start();

            System.out.println("[gRPC] LogServer started on: " + address + ":" + port);
            logServer.awaitTermination();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}