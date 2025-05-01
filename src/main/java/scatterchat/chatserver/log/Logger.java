package scatterchat.chatserver.log;

import java.util.ArrayList;
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


    public synchronized Flowable<Message> read(int lines) {
        int fromIndex = Math.max(0, this.logs.size() - lines);
        List<Message> logsCopy = new ArrayList<>(this.logs.subList(fromIndex, this.logs.size()));
        return Flowable.fromIterable(logsCopy);
    }
}