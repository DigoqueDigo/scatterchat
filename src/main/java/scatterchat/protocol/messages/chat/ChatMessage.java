package scatterchat.protocol.messages.chat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.messages.Message;


public class ChatMessage extends Message{

    private String message;


    public ChatMessage(String topic, String message){
        super(MESSAGE_TYPE.CHAT_MESSAGE, topic);
        this.message = message;
    }


    public ChatMessage(String topic, String sender, String message){
        super(MESSAGE_TYPE.CHAT_MESSAGE, topic, sender);
        this.message = message;
    }


    public String getMessage(){
        return this.message;
    }


    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ChatMessage.class);
        kryo.register(HashMap.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public static ChatMessage deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ChatMessage.class);
        kryo.register(HashMap.class);

        ChatMessage chatMessage = kryo.readObject(input, ChatMessage.class);
        input.close();

        return chatMessage;
    }


    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append("\t message: " + message);
        return buffer.toString();
    }
}