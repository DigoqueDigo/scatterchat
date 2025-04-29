package scatterchat.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.rxjava3.core.Single;
import scatterchat.LogMessageRequest;
import scatterchat.Rx3LogServiceGrpc;
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


    public void getLogs(LogMessageRequest request) {
        this.stub.getLogs(Single.just(request))
            .subscribe(
                item -> System.out.println(item.getMessage()),
                error -> error.printStackTrace(),
                () -> System.out.println("[ClientLog] log request completed")
            );
    }


    public void getMessagesOfUser(UserMessagesRequest request) {
        this.stub.getMessagesOfUser(Single.just(request))
            .subscribe(
                item -> System.out.println(item.getMessage()),
                error -> error.printStackTrace(),
                () -> System.out.println("[ClientLog] user messages request completed")
            );
    }
}