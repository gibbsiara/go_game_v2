package com.example;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ServerMain extends Application {
    ValidateUserPort validateUserPort = new ValidateUserPort();
    AlertView alertView = new AlertView();
    public void start(Stage primaryStage) {

        primaryStage.setTitle("Go Server");
        Label infoLabel = new Label("Podaj port do gry:");
        TextField portField = new TextField();
        portField.setPrefColumnCount(5);
        Button startButton = new Button("Start");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10, 10, 10, 10));
        layout.getChildren().addAll(infoLabel, portField, startButton);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));

        startButton.setOnAction(event -> {
            int port = validateUserPort.getPortFromField(portField);
            if (port != -1) {
                alertView.showAlert("Server", "Uruchamianie serwera na porcie" + port, Alert.AlertType.INFORMATION);
                GoServer.getInstance().start(port);
                startButton.setDisable(true);
            }
        });

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}