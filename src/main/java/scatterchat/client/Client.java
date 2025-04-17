package scatterchat.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.rxjava3.core.Single;
import scatterchat.LogMessageRequest;
import scatterchat.RxLogServiceGrpc;


public class Client {

    public static void main(String[] args) throws Exception {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        RxLogServiceGrpc.RxLogServiceStub logStub = Rx3LogServiceGrpc.newRxStub(channel);
        Single<LogMessageRequest> logRequest = Single.just(1).map(x -> LogMessageRequest.newBuilder().setHistory(x).build());

        logStub.getLogs(logRequest)
                .doOnComplete(() -> channel.shutdown())
                .subscribe(
                        log -> System.out.println("[gRPC LOG] " + log.getMessage()),
                        error -> error.printStackTrace(),
                        () -> System.out.println("[gRPC LOG] Stream closed"));
    }
}