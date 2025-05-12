package scatterchat.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import scatterchat.LogRequest;
import scatterchat.LogReply;
import scatterchat.Rx3LogServiceGrpc;
import scatterchat.UserLogReply;
import scatterchat.UserLogRequest;
import scatterchat.Rx3LogServiceGrpc.RxLogServiceStub;


public class ClientLog {

    private ManagedChannel channel;
    private RxLogServiceStub stub;


    public ClientLog(String loggerAddress, int loggerPort) {
        this.channel = ManagedChannelBuilder
            .forAddress(loggerAddress, loggerPort)
            .usePlaintext()
            .build();
        this.stub = Rx3LogServiceGrpc.newRxStub(this.channel);
    }


    public void shutdown() {
        this.channel.shutdown();
    }


    public Flowable<LogReply> getLogs(LogRequest request) {
        return this.stub.getLogs(Single.just(request));
    }


    public Flowable<UserLogReply> getUserLog(UserLogRequest request) {
        return this.stub.getUserLog(Single.just(request));
    }
}