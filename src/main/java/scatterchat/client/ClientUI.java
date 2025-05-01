package scatterchat.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.json.JSONObject;
import org.zeromq.ZContext;

import scatterchat.protocol.signal.ChatMessageSignal;
import scatterchat.protocol.signal.EnterTopicSignal;
import scatterchat.protocol.signal.ExitSignal;
import scatterchat.protocol.signal.ExitTopicSignal;
import scatterchat.protocol.signal.LogSignal;
import scatterchat.protocol.signal.ServerStateSignal;
import scatterchat.protocol.signal.Signal;
import scatterchat.protocol.signal.UserLogSignal;


public class ClientUI extends Application {

    private static ZContext context;
    private static JSONObject config;
    private static List<Thread> workers;
    private static BlockingQueue<Signal> signals;

    private static String topic;
    private static String username;

    private static TextArea inboxArea;
    private static TextArea infoArea;
    private static TextArea logsArea;


    public static void setupParameters(JSONObject config) {
        ClientUI.config = config;
        ClientUI.context = new ZContext();
        ClientUI.workers = new ArrayList<>();
        ClientUI.signals = new ArrayBlockingQueue<>(10);
        ClientUI.username = config.getString("username");
    }

    private void startWorkers() {
        Runnable clientCon = new ClientCon(config, context, signals);
        Runnable clientSub = new ClientSub(config, context, signals);
        ThreadFactory threadFactory = Thread.ofVirtual().factory();
        workers.add(threadFactory.newThread(clientCon));
        workers.add(threadFactory.newThread(clientSub));
        workers.forEach(worker -> worker.start());
    }


    private void waitWorkers() {
        try{
            for (Thread worker : ClientUI.workers) {
                worker.join();
            }
            ClientUI.context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public synchronized static void appendToInBox(String message) {
        Platform.runLater(() -> ClientUI.inboxArea.appendText(message + "\n"));
    }


    public synchronized static void appendToLogs(String message) {
        Platform.runLater(() -> ClientUI.logsArea.appendText(message + "\n"));
    }


    public synchronized static void appendToInfo(String message) {
        Platform.runLater(() -> ClientUI.infoArea.appendText(message + "\n"));
    }


    public synchronized static void clearInBox() {
        Platform.runLater(() -> ClientUI.inboxArea.clear());
    }


    public synchronized static void clearInfo() {
        Platform.runLater(() -> ClientUI.infoArea.clear());
    } 


    private void handleEnterTopic(String topic) throws InterruptedException {
        Signal signal = new EnterTopicSignal(topic);
        ClientUI.signals.put(signal);
    }


    private void handleExitTopic(String topic) throws InterruptedException {
        Signal signal = new ExitTopicSignal(topic);
        ClientUI.signals.put(signal);
    }


    private void handleChatMessage(String message) throws InterruptedException {
        Signal signal = new ChatMessageSignal(ClientUI.username, ClientUI.topic, message);
        ClientUI.signals.put(signal);
    }


    private void handleUserLog(String username, int lines) throws InterruptedException {
        Signal signal = new UserLogSignal(username, ClientUI.topic, lines);
        ClientUI.signals.put(signal);
    }


    private void handleLog(int lines) throws InterruptedException {
        Signal signal = new LogSignal(ClientUI.topic, lines);
        ClientUI.signals.put(signal);
    }


    private void handleServerState() throws InterruptedException{
        Signal signal = new ServerStateSignal();
        ClientUI.signals.put(signal);
    }


    private void handleExit() throws InterruptedException {
        Signal signal = new ExitSignal();
        ClientUI.signals.put(signal);
    }


    private boolean checkTopic() {
        if (ClientUI.topic == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Unknown Topic");
            alert.setContentText("You have to select a topic.");
            alert.showAndWait();
        }
        return ClientUI.topic != null;
    }


    private void handleTopicFieldEnter(TextField topicField) {
        try {
            String topic = topicField.getText().trim();
            System.out.println("Selected topic: " + topic);

            if (topic.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Invalid Topic");
                alert.setContentText("Empty topics are not allowed.");
                alert.showAndWait();
                return;
            }

            if (ClientUI.topic != null)
                handleExitTopic(ClientUI.topic);

            ClientUI.topic = topic;
            handleEnterTopic(ClientUI.topic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleMessageFieldEnter(TextField messageField) {
        try {
            if (checkTopic()) {
                String message = messageField.getText().trim();
                this.handleChatMessage(message);
                messageField.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleServerStateButtonClick() {
        try {
            if (checkTopic()){
                this.handleServerState();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleExitEvent() {
        try {
            if (ClientUI.topic != null) {
                handleExitTopic(ClientUI.topic);
            }
            handleExit();
            waitWorkers();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleSubmitButtonClick(String linesText, String username, Stage popup) {
        try {
            int lines = Integer.MAX_VALUE;

            if (linesText.matches("\\d+"))
                lines = Integer.parseInt(linesText);

            if (username.isEmpty())
                handleLog(lines);
            else
                handleUserLog(username, lines);

            popup.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleHistoryButtonClick() {
        if (checkTopic()) {
            Stage popupStage = new Stage();
            popupStage.setTitle("Message History Form");
            popupStage.initModality(Modality.APPLICATION_MODAL);

            GridPane formGrid = new GridPane();
            formGrid.setVgap(10);
            formGrid.setHgap(10);

            TextField linesField = new TextField();
            linesField.setPromptText("Lines");
            linesField.getStyleClass().add("text-field");

            TextField usernameField = new TextField();
            usernameField.setPromptText("User");
            usernameField.getStyleClass().add("text-field");

            Button submitButton = new Button("Submit");
            submitButton.getStyleClass().add("button");
            submitButton.setMaxWidth(Double.MAX_VALUE);
            submitButton.setOnAction(event ->
                handleSubmitButtonClick(
                    linesField.getText(),
                    usernameField.getText(),
                    popupStage
            ));

            GridPane.setHgrow(linesField, Priority.ALWAYS);
            GridPane.setHgrow(usernameField, Priority.ALWAYS);
            GridPane.setHgrow(submitButton, Priority.ALWAYS);

            formGrid.setPadding(new Insets(10));
            formGrid.add(linesField, 0, 0);
            formGrid.add(usernameField, 0, 1);
            formGrid.add(submitButton, 0, 2);

            Scene scene = new Scene(formGrid, 300, 200);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            popupStage.setScene(scene);
            popupStage.showAndWait();
        }
    }


    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("ScatterChat");
        primaryStage.setOnCloseRequest(e -> handleExitEvent());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();

        col1.setHgrow(Priority.ALWAYS);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        TextField topicField = new TextField();
        topicField.setPromptText("Enter topic");
        topicField.getStyleClass().add("text-field");
        topicField.setOnAction(e -> handleTopicFieldEnter(topicField));

        TextField messageField = new TextField();
        messageField.setPromptText("Enter message");
        messageField.getStyleClass().add("text-field");
        messageField.setOnAction(e -> handleMessageFieldEnter(messageField));

        Button historyButton = new Button("Message history");
        historyButton.getStyleClass().add("button");
        historyButton.setMaxWidth(Double.MAX_VALUE);
        historyButton.setOnAction(e -> handleHistoryButtonClick());

        Button serverStateButton = new Button("Server State");
        serverStateButton.getStyleClass().add("button");
        serverStateButton.setMaxWidth(Double.MAX_VALUE);
        serverStateButton.setOnAction(e -> handleServerStateButtonClick());

        Label inboxLabel = new Label("Inbox");
        inboxLabel.getStyleClass().add("label");

        Label infoLabel = new Label("Info");
        infoLabel.getStyleClass().add("label");

        Label logsLabel = new Label("Logs");
        logsLabel.getStyleClass().add("label");

        StackPane inboxBox = new StackPane(inboxLabel);
        inboxBox.getStyleClass().add("bordered-pane");

        StackPane infoBox = new StackPane(infoLabel);
        infoBox.getStyleClass().add("bordered-pane");

        StackPane logsBox = new StackPane(logsLabel);
        logsBox.getStyleClass().add("bordered-pane");

        TextArea inboxArea = new TextArea();
        inboxArea.setEditable(false);

        TextArea infoArea = new TextArea();
        infoArea.setEditable(false);

        TextArea logsArea = new TextArea();
        logsArea.setEditable(false);

        grid.add(topicField, 0, 0);
        grid.add(messageField, 0, 1);
        grid.add(historyButton, 1, 0);
        grid.add(serverStateButton, 1, 1);

        grid.add(inboxBox, 0, 2);
        grid.add(infoBox, 1, 2);
        grid.add(logsBox, 0, 4);

        grid.add(inboxArea, 0, 3);
        grid.add(infoArea, 1, 3);
        grid.add(logsArea, 0, 5);

        GridPane.setColumnSpan(logsBox, 2);
        GridPane.setColumnSpan(logsArea, 2);

        GridPane.setHgrow(topicField, Priority.ALWAYS);
        GridPane.setHgrow(messageField, Priority.ALWAYS);
        GridPane.setHgrow(historyButton, Priority.ALWAYS);
        GridPane.setHgrow(serverStateButton, Priority.ALWAYS);

        GridPane.setVgrow(inboxArea, Priority.ALWAYS);
        GridPane.setHgrow(inboxArea, Priority.ALWAYS);

        GridPane.setVgrow(infoArea, Priority.ALWAYS);
        GridPane.setHgrow(infoArea, Priority.ALWAYS);

        GridPane.setVgrow(logsArea, Priority.ALWAYS);
        GridPane.setHgrow(logsArea, Priority.ALWAYS);

        RowConstraints rowGrow = new RowConstraints();
        rowGrow.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(
            new RowConstraints(),
            new RowConstraints(),
            new RowConstraints(),
            rowGrow,
            new RowConstraints(),
            rowGrow
        );

        Scene scene = new Scene(grid, 600, 400);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);

        ClientUI.inboxArea = inboxArea;
        ClientUI.infoArea = infoArea;
        ClientUI.logsArea = logsArea;

        startWorkers();
        primaryStage.show();
    }
}