package scatterchat.protocol.messages.chat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.messages.Message;


public class ChatExitMessage extends Message{


    public ChatExitMessage(String topic, String sender){
        super(MESSAGE_TYPE.CHAT_EXIT_MESSAGE, topic, sender);
    }


    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ChatExitMessage.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public static ChatExitMessage deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ChatExitMessage.class);

        ChatExitMessage chatExitMessage = kryo.readObject(input, ChatExitMessage.class);
        input.close();

        return chatExitMessage;
    }
}