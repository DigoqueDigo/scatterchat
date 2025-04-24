package scatterchat.protocol.carrier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.json.JSONObject;

import scatterchat.protocol.message.dht.DHTRep;
import scatterchat.protocol.message.dht.IJson;


public class JSONCarrier {

    private BufferedReader inputStream;
    private PrintWriter outputStream;


    public JSONCarrier(Socket socket) throws IOException {
        this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.outputStream = new PrintWriter(socket.getOutputStream(), true);
    }


    public void send(IJson request) throws IOException {
        System.out.println("A ENVIAR " + request);
        this.outputStream.println(request.toJson().toString());
    }


    public DHTRep receive() throws IOException {
        return new DHTRep(new JSONObject(this.inputStream.readLine()));
    }
}