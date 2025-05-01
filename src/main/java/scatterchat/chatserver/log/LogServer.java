package scatterchat.chatserver.log;

import org.json.JSONObject;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import scatterchat.LogReply;
import scatterchat.LogRequest;
import scatterchat.Rx3LogServiceGrpc;
import scatterchat.UserLogReply;
import scatterchat.UserLogRequest;
import scatterchat.protocol.message.Message.MessageType;
import scatterchat.protocol.message.chat.ChatMessage;


public class LogServer extends Rx3LogServiceGrpc.LogServiceImplBase implements Runnable {

    private Logger logger;
    private JSONObject config;


    public LogServer(JSONObject config, Logger logger) {
        this.logger = logger;
        this.config = config;
    }


    @Override
    public Flowable<LogReply> getLogs(Single<LogRequest> request) {
        return request.flatMapPublisher(req -> {
            return this.logger
                .read(req.getLines())
                .doOnComplete(() -> System.out.println(req.getLines() + ", " + req.getTopic()))
                .filter(m -> m.getType().equals(MessageType.CHAT_MESSAGE))
                .map(m -> (ChatMessage) m)
                .filter(m -> m.getTopic().equals(req.getTopic()))
                .map(m -> LogReply.newBuilder().setMessage(m.getMessage()).setClient(m.getClient()).build());
        });
    }


    @Override
    public Flowable<UserLogReply> getUserLog(Single<UserLogRequest> request) {
        return request.flatMapPublisher(req -> {
            return this.logger
                .read(Integer.MAX_VALUE)
                .doOnComplete(() -> System.out.println(req.getClient() + ", " + req.getLines() + ", " + req.getTopic()))
                .filter(m -> m.getType().equals(MessageType.CHAT_MESSAGE))
                .map(m -> (ChatMessage) m)
                .filter(m -> m.getClient().equals(req.getClient()) && m.getTopic().equals(req.getTopic()))
                .takeLast(req.getLines())
                .map(m -> UserLogReply.newBuilder().setMessage(m.getMessage()).build());
        });
    }


    @Override
    public void run() {

        try {
            int logPort = config.getInt("logPort");
            System.out.println("[LogServer] started");
            System.out.println("[LogServer] bind: localhost:" + logPort);

            Server logServer = ServerBuilder
                .forPort(logPort)
                .addService(new LogServer(this.config, this.logger))
                .build()
                .start();

            logServer.awaitTermination();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }
}