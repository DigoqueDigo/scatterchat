package scatterchat.chatserver.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import scatterchat.protocol.message.Message;


public class Logger {

    private List<Message> logs;


    public Logger() {
        this.logs = new ArrayList<>();
    }


    public synchronized void write(Message message) {
        this.logs.add(message);
    }


    public synchronized Flowable<Message> read() {
        List<Message> logsCopy = new ArrayList<>(this.logs);
        Collections.reverse(logsCopy);
        return Flowable.fromIterable(logsCopy);
    }
}