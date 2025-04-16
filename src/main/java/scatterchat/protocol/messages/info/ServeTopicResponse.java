package scatterchat.protocol.messages.info;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import scatterchat.protocol.messages.Message;


public class ServeTopicResponse extends Message{

    private boolean success;


    public ServeTopicResponse(boolean success){
        super(MESSAGE_TYPE.SERVE_TOPIC_RESPONSE);
        this.success = success;
    }


    public boolean getSuccess(){
        return this.success;
    }


    public byte[] serialize(){

        Kryo kryo = new Kryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ServeTopicResponse.class);
        kryo.writeObject(output, this);

        output.flush();
        output.close();

        return byteArrayOutputStream.toByteArray();
    }


    public static ServeTopicResponse deserialize(byte[] data){

        Kryo kryo = new Kryo();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        kryo.register(Message.MESSAGE_TYPE.class);
        kryo.register(ServeTopicResponse.class);

        ServeTopicResponse serveTopicResponse = kryo.readObject(input, ServeTopicResponse.class);
        input.close();

        return serveTopicResponse;
    }


    public String toString(){
        StringBuilder buffer = new StringBuilder();
        buffer.append(super.toString());
        buffer.append("\t success: " + success);
        return buffer.toString();
    }
}