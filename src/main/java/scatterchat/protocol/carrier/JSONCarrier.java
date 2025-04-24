package scatterchat.protocol.carrier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.json.JSONObject;


public class JSONCarrier {

    private DataInputStream inputStream;
    private DataOutputStream outputStream;


    public JSONCarrier(Socket socket) throws IOException {
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
    }


    public void send(JSONObject data) throws IOException{
        this.outputStream.writeUTF(data.toString());
    }


    public JSONObject receive() throws IOException{
        return new JSONObject(this.inputStream.readUTF());
    }
}