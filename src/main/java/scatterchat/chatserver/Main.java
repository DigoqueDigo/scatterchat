package scatterchat.chatserver;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import scatterchat.protocol.messages.Message;


public class Main{
    
    public static void main(String[] args){

        BlockingQueue<Message> broadcast = new ArrayBlockingQueue<>(10);
        BlockingQueue<Message> delivered = new ArrayBlockingQueue<>(10);

        ChatServerExtPull chatServerExtPull = new ChatServerExtPull(null, delivered, broadcast);

    }
}