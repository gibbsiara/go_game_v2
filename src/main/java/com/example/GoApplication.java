package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;


public class GoApplication extends Application {

    private GoClient client;
    private Stage primaryStage;
    private BorderPane gameRoot;
    private TextArea messageArea;
    private Pane boardPane;
    private int currentBoardSize = 0;
    private AlertView alertView;
    private Circle[][] stones;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.client = new GoClient(this);
        this.alertView = new AlertView();

        showMainMenu();
    }

    private void showMainMenu() {
        VBox menuBox = new VBox(15);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setPadding(new Insets(20));

        Label title = new Label("Gra w Go");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TitledPane hostPane = new TitledPane();
        hostPane.setText("Stwórz Serwer");
        hostPane.setCollapsible(false);
        VBox hostContent = new VBox(10);
        TextField hostPortField = new TextField("12345");
        hostPortField.setPromptText("Port");
        ComboBox<Integer> sizeBox = new ComboBox<>();
        sizeBox.getItems().addAll(9, 13, 19);
        sizeBox.setValue(19);
        Button startServerBtn = new Button("Uruchom serwer i dołącz do gry");

        hostContent.getChildren().addAll(new Label("Port:"), hostPortField, new Label("Rozmiar:"), sizeBox, startServerBtn);
        hostPane.setContent(hostContent);

        TitledPane joinPane = new TitledPane();
        joinPane.setText("Dołącz do gry");
        joinPane.setCollapsible(false);
        VBox joinContent = new VBox(10);
        TextField joinPortField = new TextField("12345");
        ComboBox<Integer> joinSizeBox = new ComboBox<>();
        joinSizeBox.getItems().addAll(9, 13, 19);
        joinSizeBox.setValue(19);
        Button joinBtn = new Button("Połącz");

        joinContent.getChildren().addAll( new Label("Rozmiar:"), joinSizeBox, new Label("Port:"), joinPortField, joinBtn);
        joinPane.setContent(joinContent);

        menuBox.getChildren().addAll(title, hostPane, joinPane);

        startServerBtn.setOnAction(e -> {
            try {
                int port = Integer.parseInt(hostPortField.getText());
                int size = sizeBox.getValue();
                GoServer.getInstance().start(port, size);
                connectToServer("localhost", port, size);
            } catch (Exception ex) {
                alertView.showAlert("Błąd", "Błąd uruchamiania serwera: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        joinBtn.setOnAction(e -> {
            try {
                int port = Integer.parseInt(joinPortField.getText());
                int size = sizeBox.getValue();
                connectToServer("localhost", port, size);
            } catch (Exception ex) {
                alertView.showAlert("Błąd", "Błąd połączenia: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        Scene scene = new Scene(menuBox, 400, 500);
        primaryStage.setTitle("Go Game Launcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void connectToServer(String ip, int port, int initialSize) {
        try {
            client.connect(ip, port);
            buildGameScreen(initialSize);
        } catch (Exception e) {
            alertView.showAlert("Błąd", "Nie udało się połączyć: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void buildGameScreen(int size) {
        this.currentBoardSize = size;
        gameRoot = new BorderPane();

        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));

        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setPrefWidth(200);
        messageArea.setPrefHeight(400);

        Button passBtn = new Button("Pasuj");
        passBtn.setMaxWidth(Double.MAX_VALUE);
        passBtn.setOnAction(e -> client.sendPass());

        Button surrenderBtn = new Button("Poddaj się");
        surrenderBtn.setMaxWidth(Double.MAX_VALUE);
        surrenderBtn.setOnAction(e -> client.sendSurrender());

        Button resumeBtn = new Button("Wznów grę (Spór)");
        resumeBtn.setMaxWidth(Double.MAX_VALUE);
        resumeBtn.setOnAction(e -> client.sendResume());

        rightPanel.getChildren().addAll(messageArea, passBtn, surrenderBtn, resumeBtn);
        gameRoot.setRight(rightPanel);

        createBoardView(size);
        gameRoot.setCenter(boardPane);

        Scene gameScene = new Scene(gameRoot, 850, 600);
        primaryStage.setScene(gameScene);
        primaryStage.centerOnScreen();
    }

    private void createBoardView(int size) {
        int cellSize = 30;
        boardPane = new Pane();
        boardPane.setStyle("-fx-background-color: #DEB887;");

        stones = new Circle[size][size];

        for (int i = 0; i < size; i++) {
            Line hLine = new Line(cellSize, (i + 1) * cellSize, size * cellSize, (i + 1) * cellSize);
            Line vLine = new Line((i + 1) * cellSize, cellSize, (i + 1) * cellSize, size * cellSize);
            boardPane.getChildren().addAll(hLine, vLine);
        }

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Rectangle clickArea = new Rectangle(cellSize, cellSize, Color.TRANSPARENT);
                clickArea.setX((x + 0.5) * cellSize);
                clickArea.setY((y + 0.5) * cellSize);

                Circle stone = new Circle((x + 1) * cellSize, (y + 1) * cellSize, cellSize * 0.4);
                stone.setFill(Color.TRANSPARENT);
                stone.setStroke(Color.TRANSPARENT);
                stones[x][y] = stone;

                int finalX = x;
                int finalY = y;

                clickArea.setOnMouseClicked(e -> {
                    client.sendMove(finalX, finalY);
                });

                boardPane.getChildren().addAll(stone, clickArea);
            }
        }

        boardPane.setPrefSize((size + 1) * cellSize + 20, (size + 1) * cellSize + 20);
    }


    public void updateBoard(String boardData) {

        String[] fields = boardData.split(";");
        int totalFields = fields.length;
        int calculatedSize = (int) Math.sqrt(totalFields);

        if (calculatedSize != currentBoardSize) {
            currentBoardSize = calculatedSize;
            createBoardView(currentBoardSize);
            gameRoot.setCenter(boardPane);
        }

        for (int i = 0; i < totalFields; i++) {
            int x = i % currentBoardSize;
            int y = i / currentBoardSize;

            String field = fields[i];
            Circle stone = stones[x][y];

            if (field.equals("BLACK")) {
                stone.setFill(Color.BLACK);
                stone.setStroke(Color.BLACK);
            } else if (field.equals("WHITE")) {
                stone.setFill(Color.WHITE);
                stone.setStroke(Color.BLACK);
            } else {
                stone.setFill(Color.TRANSPARENT);
                stone.setStroke(Color.TRANSPARENT);
            }
        }
    }

    public void appendLog(String message) {
        messageArea.appendText(message + "\n");
    }

}