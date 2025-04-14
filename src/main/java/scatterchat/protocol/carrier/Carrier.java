package scatterchat.protocol.carrier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.zeromq.ZMQ;
import scatterchat.protocol.messages.Message;
import scatterchat.protocol.messages.Message.MESSAGE_TYPE;


public final class Carrier{

    private ZMQ.Socket socket;
    private Map<MESSAGE_TYPE, Function<byte[], Message>> deserializers;


    public Carrier(ZMQ.Socket socket){
        this.socket = socket;
        this.deserializers = new HashMap<>();
    }


    public void register(ZMQ.Socket socket){
        this.socket = socket;
    }


    public void on(MESSAGE_TYPE type, Function<byte[], Message> deserializer){
        this.deserializers.put(type, deserializer);
    }


    public void send(Message message){
        socket.sendMore(message.getType().name());
        socket.send(message.serialize());
    }


    public Message receive(){
        MESSAGE_TYPE type = MESSAGE_TYPE.valueOf(new String (socket.recv()));
        return deserializers.get(type).apply(socket.recv());
    }


    public void sendWithTopic(Message message){
        socket.sendMore(message.getTopic());
        send(message);
    }


    public Message receiveWithTopic(){
        socket.recv();
        return receive();
    }
}