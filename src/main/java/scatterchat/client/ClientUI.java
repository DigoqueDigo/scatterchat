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
import scatterchat.protocol.signal.UserLogSignal;


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


    private void handleEnterTopic(String input) throws InterruptedException {
        Signal signal = new EnterTopicSignal(this.topic);
        this.signals.put(signal);
    }


    private void handleExitTopic(String input) throws InterruptedException {
        Signal signal = new ExitTopicSignal(this.topic);
        this.signals.put(signal);
    }


    private void handleChatMessage(String input) throws InterruptedException {
        Signal signal = new ChatMessageSignal(this.username, this.topic, input);
        this.signals.put(signal);
    }


    private void handleUserLog(String input) throws InterruptedException {
        String[] parts = input.split(" ");
        Signal signal = new UserLogSignal(parts[parts.length - 1], this.topic);
        this.signals.put(signal);
    }


    private void handleInfo(String input) throws InterruptedException{
        Signal signal = new ServerStateSignal();
        this.signals.put(signal);
    }


    private void handleLog(String input) throws InterruptedException {
        String[] parts = input.split(" ");
        int history = (parts.length > 1) ? Integer.parseInt(parts[parts.length - 1]) : Integer.MAX_VALUE;
        Signal signal = new LogSignal(this.topic, history);
        this.signals.put(signal);
    }


    private void handleExit() throws InterruptedException {
        Signal signal = new ExitSignal();
        this.signals.put(signal);
        System.out.println("Bye");
    }


    public void run(){

        try {

            Thread.sleep(500);

            String input;
            this.username = this.config.getString("username");
            String prompt = String.format("%s >>> ", this.username);

            while (true) {

                this.topic = this.lineReader.readLine(prompt);
                this.handleEnterTopic(this.topic);
                String topicPrompt = String.format("[%s] %s >>> ", this.topic, this.username);

                Thread.sleep(1000);

                while ((input = this.lineReader.readLine(topicPrompt)) != null) {
                    String command = input.split(" ")[0];
                    switch (command) {
                        case "/log" -> handleLog(input);
                        case "/info" -> handleInfo(input);
                        case "/exit" -> handleExitTopic(input);
                        case "/userlog" -> handleUserLog(input);
                        default -> handleChatMessage(input);
                    }

                    if (command.equals("/exit")) {
                        break;
                    }

                    Thread.sleep(500);
                }

                Thread.sleep(500);
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