package scatterchat.client;

import java.util.concurrent.BlockingQueue;

import javafx.application.Application;
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

import scatterchat.protocol.signal.ChatMessageSignal;
import scatterchat.protocol.signal.EnterTopicSignal;
import scatterchat.protocol.signal.ExitSignal;
import scatterchat.protocol.signal.ExitTopicSignal;
import scatterchat.protocol.signal.LogSignal;
import scatterchat.protocol.signal.ServerStateSignal;
import scatterchat.protocol.signal.Signal;
import scatterchat.protocol.signal.UserLogSignal;


public class ClientUI extends Application {

    private static String topic;
    private static String username;
    private static BlockingQueue<Signal> signals;


    // Setters for static fields
    public static void setParameters(JSONObject config, BlockingQueue<Signal> signals) {
        ClientUI.username = config.getString("username");
        ClientUI.signals = signals;
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


    private void handleUserLog(String username, int historySize) throws InterruptedException {
        Signal signal = new UserLogSignal(username, ClientUI.topic);
        ClientUI.signals.put(signal);
    }


    private void handleLog(int historySize) throws InterruptedException {
        Signal signal = new LogSignal(ClientUI.topic, historySize);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleSubmitButtonClick(String history, String username, Stage popup) {
        try {
            int historySize = Integer.MAX_VALUE;

            if (!history.isEmpty())
                historySize = Integer.parseInt(history);

            if (username.isEmpty())
                handleLog(historySize);
            else
                handleUserLog(username, historySize);

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

            TextField historyField = new TextField();
            historyField.setPromptText("History");
            historyField.getStyleClass().add("text-field");

            TextField usernameField = new TextField();
            usernameField.setPromptText("User");
            usernameField.getStyleClass().add("text-field");

            Button submitButton = new Button("Submit");
            submitButton.getStyleClass().add("button");
            submitButton.setMaxWidth(Double.MAX_VALUE);
            submitButton.setOnAction(event ->
                handleSubmitButtonClick(
                    historyField.getText(),
                    usernameField.getText(),
                    popupStage
            ));

            GridPane.setHgrow(historyField, Priority.ALWAYS);
            GridPane.setHgrow(usernameField, Priority.ALWAYS);
            GridPane.setHgrow(submitButton, Priority.ALWAYS);

            formGrid.setPadding(new Insets(10));
            formGrid.add(historyField, 0, 0);
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
        infoArea.setEditable(true);

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
        grid.getRowConstraints().addAll(new RowConstraints(), new RowConstraints(), new RowConstraints(), rowGrow);

        Scene scene = new Scene(grid, 600, 400);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);

        System.out.println(ClientUI.username);
        System.out.println(ClientUI.topic);
        primaryStage.show();
    }
}