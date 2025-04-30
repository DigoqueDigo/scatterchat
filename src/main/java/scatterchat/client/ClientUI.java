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
import scatterchat.protocol.signal.ServeStateSignal;
import scatterchat.protocol.signal.Signal;
import scatterchat.protocol.signal.UserMessagesSignal;


public class ClientUI implements Runnable {

    private JSONObject config;
    private BlockingQueue<Signal> signals;

    private String username;
    private String currentTopic;

    private Terminal terminal;
    private LineReader lineReader;


    public ClientUI(JSONObject config, BlockingQueue<Signal> signals) throws IOException {
        this.config = config;
        this.signals = signals;
        this.terminal = TerminalBuilder.builder().dumb(true).build();
        this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
    }


    public void handleEnterTopic(String input) throws InterruptedException {
        this.currentTopic = input.strip();
        Signal signal = new EnterTopicSignal(this.currentTopic);
        this.signals.put(signal);
    }


    public void handleExitTopic(String input) throws InterruptedException {
        Signal signal = new ExitTopicSignal(this.currentTopic);
        this.signals.put(signal);
        this.currentTopic = null;
    }


    public void handleChatMessage(String input) throws InterruptedException {
        Signal signal = new ChatMessageSignal(this.username, this.currentTopic, input);
        this.signals.put(signal);
    }


    public void handleUsers(String input) throws InterruptedException {
        String[] parts = input.split(" ");
        Signal signal = new UserMessagesSignal(parts[parts.length - 1], this.currentTopic);
        this.signals.put(signal);
    }


    public void handleInfo(String input) throws InterruptedException{
        Signal signal = new ServeStateSignal();
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

            String username = this.config.getString("username");
            String prompt = String.format("%s >>> ", username);

            while (true) {

                String input;
                String topic = this.lineReader.readLine(prompt);
                String topicPrompt = String.format("[%s] %s >>> ", topic, username);

                this.handleEnterTopic(topic);

                while ((input = this.lineReader.readLine(topicPrompt)) != null) {
                    String command = input.split(" ")[0];
                    switch (command) {
                        case "/info" -> handleInfo(input);
                        case "/users" -> handleUsers(input);
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