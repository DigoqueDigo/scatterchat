package scatterchat.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import scatterchat.LogMessageRequest;
import scatterchat.LogMessageReply;
import scatterchat.Rx3LogServiceGrpc;
import scatterchat.UserMessagesReply;
import scatterchat.UserMessagesRequest;
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


    public Flowable<LogMessageReply> getLogs(LogMessageRequest request) {
        return this.stub.getLogs(Single.just(request));
    }


    public Flowable<UserMessagesReply> getMessagesOfUser(UserMessagesRequest request) {
        return this.stub.getMessagesOfUser(Single.just(request));
    }
}