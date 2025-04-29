package scatterchat.chatserver.log;

import org.json.JSONObject;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import scatterchat.LogMessageReply;
import scatterchat.LogMessageRequest;
import scatterchat.Rx3LogServiceGrpc;
import scatterchat.UserMessagesReply;
import scatterchat.UserMessagesRequest;
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
    public Flowable<LogMessageReply> getLogs(Single<LogMessageRequest> request) {
        return request.flatMapPublisher(req -> {
            synchronized (this.logger) {
                return this.logger
                    .read(req.getHistory())
                    .map(m -> LogMessageReply.newBuilder().setMessage(m.toString()).build());
            }
        });
    }


    @Override
    public Flowable<UserMessagesReply> getMessagesOfUser(Single<UserMessagesRequest> request) {
        return request.flatMapPublisher(req -> {
            synchronized (this.logger) {
                return this.logger
                    .read(-1)
                    .filter(m -> m.getType().equals(MessageType.CHAT_MESSAGE))
                    .map(m -> (ChatMessage) m)
                    .filter(m -> m.getClient().equals(req.getUsername()) && m.getTopic().equals(req.getTopic()))
                    .map(m -> UserMessagesReply.newBuilder().setMessage(m.getMessage()).build());
            }
        });
    }


    @Override
    public void run() {

        try {
            int logPort = config.getInt("logPort");
            System.out.println("[LogServer] started");
            System.out.println("[LogServer] bind port: " + logPort);

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