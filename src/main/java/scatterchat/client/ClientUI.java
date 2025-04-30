package scatterchat.client;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.json.JSONObject;

import scatterchat.protocol.signal.ChatMessageSignal;
import scatterchat.protocol.signal.EnterTopicSignal;
import scatterchat.protocol.signal.ExitSignal;
import scatterchat.protocol.signal.ExitTopicSignal;
import scatterchat.protocol.signal.LogSignal;
import scatterchat.protocol.signal.ServerStateSignal;
import scatterchat.protocol.signal.Signal;
import scatterchat.protocol.signal.UserMessagesSignal;


public class ClientUI implements Runnable {

    private JSONObject config;
    private BlockingQueue<Signal> signals;

    private String topic;
    private String username;

    private Terminal terminal;
    private LineReader lineReader;


    public ClientUI(JSONObject config, BlockingQueue<Signal> signals) throws IOException {
        this.config = config;
        this.signals = signals;
        this.terminal = TerminalBuilder.builder().dumb(true).build();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }


    public void handleEnterTopic(String input) throws InterruptedException {
        Signal signal = new EnterTopicSignal(this.topic);
        this.signals.put(signal);
    }


    public void handleExitTopic(String input) throws InterruptedException {
        Signal signal = new ExitTopicSignal(this.topic);
        this.signals.put(signal);
    }


    public void handleChatMessage(String input) throws InterruptedException {
        Signal signal = new ChatMessageSignal(this.username, this.topic, input);
        this.signals.put(signal);
    }


    public void handleUsers(String input) throws InterruptedException {
        String[] parts = input.split(" ");
        Signal signal = new UserMessagesSignal(parts[parts.length - 1], this.topic);
        this.signals.put(signal);
    }


    public void handleInfo(String input) throws InterruptedException{
        Signal signal = new ServerStateSignal();
        this.signals.put(signal);
    }


    public void handleLog(String input) throws InterruptedException {
        String[] parts = input.split(" ");
        Signal signal = new LogSignal(Integer.parseInt(parts[parts.length - 1]));
        this.signals.put(signal);
    }


    public void handleExit() throws InterruptedException {
        Signal signal = new ExitSignal();
        this.signals.put(signal);
        System.out.println("Bye");
    }


    public void run(){

        try {

            String input;
            this.username = this.config.getString("username");
            String prompt = String.format("%s >>> ", this.username);

            while (true) {

                this.topic = this.lineReader.readLine(prompt);
                this.handleEnterTopic(this.topic);
                String topicPrompt = String.format("[%s] %s >>> ", this.topic, this.username);

                while ((input = this.lineReader.readLine(topicPrompt)) != null) {
                    String command = input.split(" ")[0];
                    switch (command) {
                        case "/info" -> handleInfo(input);
                        case "/user" -> handleUsers(input);
                        case "/log" -> handleLog(input);
                        case "/exit" -> handleExitTopic(input);
                        default -> handleChatMessage(input);
                    }

                    if (command.equals("/exit")) {
                        break;
                    }
                }
            }
        }

        catch (EndOfFileException e) {
            try {
                this.handleExit();
            } catch (Exception f) {
                f.printStackTrace();
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}