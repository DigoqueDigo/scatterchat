package scatterchat;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class ChatUIFX extends Application {

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle("ScatterChat");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();

        col1.setHgrow(Priority.ALWAYS);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Topic
        TextField topicField = new TextField();
        topicField.setPromptText("Enter topic");
        topicField.getStyleClass().add("text-field");
        GridPane.setHgrow(topicField, Priority.ALWAYS);


        topicField.setOnAction(e -> {
            String value = topicField.getText();
            System.out.println("Enter pressed. Value: " + value);
            if (value.isEmpty() || value.equals("exit")){
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Input Received");
                alert.setHeaderText(null); // You can add a header if you want
                alert.setContentText("You entered: " + value);
            
                alert.showAndWait();

            }
            // You can add validation, submit logic, etc. here
        });

        // Message
        TextField messageField = new TextField();
        messageField.setPromptText("Enter message");
        messageField.getStyleClass().add("text-field");
        GridPane.setHgrow(messageField, Priority.ALWAYS);

        grid.add(topicField, 0, 0);
        grid.add(messageField, 0, 1);

        // Buttons
        Button seeLogsButton = new Button("See Logs");
        seeLogsButton.getStyleClass().add("button");
        seeLogsButton.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(seeLogsButton, Priority.ALWAYS);



        seeLogsButton.setOnAction(e -> {

            Stage popupStage = new Stage();
            popupStage.setTitle("Logs Form");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            
            GridPane formGrid = new GridPane();
            formGrid.setVgap(10);
            formGrid.setHgap(10);

            TextField historyField = new TextField();
            historyField.setPromptText("History");
            historyField.getStyleClass().add("text-field");
            GridPane.setHgrow(historyField, Priority.ALWAYS);
            
            TextField userField = new TextField();
            userField.setPromptText("User");
            userField.getStyleClass().add("text-field");
            GridPane.setHgrow(userField, Priority.ALWAYS);

            Button submitButton = new Button("Submit");
            submitButton.getStyleClass().add("button");
            submitButton.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(submitButton, Priority.ALWAYS);
            submitButton.setOnAction(event -> {
                String name = historyField.getText();
                String email = userField.getText();
                System.out.println("Submitted: " + name + ", " + email);
                popupStage.close();
            });

            // Arrange form in a grid
            formGrid.setPadding(new Insets(10));

            formGrid.add(historyField, 0, 0);
            formGrid.add(userField, 0, 1);
            formGrid.add(submitButton, 0, 2);

            // Show popup
            Scene scene = new Scene(formGrid, 300, 200);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            popupStage.setScene(scene);
            popupStage.showAndWait();
        });









        Button seeServerStateButton = new Button("See Server State");
        seeServerStateButton.getStyleClass().add("button");
        seeServerStateButton.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(seeServerStateButton, Priority.ALWAYS);

        grid.add(seeLogsButton, 1, 0);
        grid.add(seeServerStateButton, 1, 1);

        // Labels with bordered containers
        Label inboxLabel = new Label("Inbox");
        inboxLabel.getStyleClass().add("label");
        StackPane inboxBox = new StackPane(inboxLabel);
        inboxBox.getStyleClass().add("bordered-pane");

        Label infoLabel = new Label("Info");
        infoLabel.getStyleClass().add("label");
        StackPane infoBox = new StackPane(infoLabel);
        infoBox.getStyleClass().add("bordered-pane");

        Label logsLabel = new Label("Logs");
        logsLabel.getStyleClass().add("label");
        StackPane logsBox = new StackPane(logsLabel);
        logsBox.getStyleClass().add("bordered-pane");

        grid.add(inboxBox, 0, 2);
        grid.add(infoBox, 1, 2);
        grid.add(logsBox, 0, 4);
        GridPane.setColumnSpan(logsBox, 2);

        // TextAreas
        TextArea inboxArea = new TextArea();
        inboxArea.setEditable(false);
        GridPane.setVgrow(inboxArea, Priority.ALWAYS);
        GridPane.setHgrow(inboxArea, Priority.ALWAYS);

        TextArea infoArea = new TextArea();
        infoArea.setEditable(true);
        GridPane.setVgrow(infoArea, Priority.ALWAYS);
        GridPane.setHgrow(infoArea, Priority.ALWAYS);

        TextArea logsArea = new TextArea();
        logsArea.setEditable(false);
        GridPane.setVgrow(logsArea, Priority.ALWAYS);
        GridPane.setHgrow(logsArea, Priority.ALWAYS);

        RowConstraints rowGrow = new RowConstraints();
        rowGrow.setVgrow(Priority.ALWAYS);
        grid.getRowConstraints().addAll(new RowConstraints(), new RowConstraints(), new RowConstraints(), rowGrow);

        grid.add(inboxArea, 0, 3);
        grid.add(infoArea, 1, 3);
        grid.add(logsArea, 0, 5);
        GridPane.setColumnSpan(logsArea, 2);

        primaryStage.setOnCloseRequest(event -> System.out.println("Window is closing!"));

        Scene scene = new Scene(grid, 600, 400);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
