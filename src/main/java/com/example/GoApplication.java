package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.IOException;

public class GoApplication extends Application {

    private GoClient client;
    private Stage primaryStage;
    private BorderPane gameRoot;
    private TextArea messageArea;
    private Pane boardPane;
    private int currentBoardSize = 0;
    private AlertView alertView;
    private Circle[][] stones;

    private static final int CELL_SIZE = 35;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.alertView = new AlertView();
        this.client = new GoClient(this);

        showMainMenu();
    }

    private void showMainMenu() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f4f4f4; -fx-font-family: 'Arial';");

        Label titleLabel = new Label("Gra w GO");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #333;");

        GridPane configGrid = new GridPane();
        configGrid.setHgap(10);
        configGrid.setVgap(10);
        configGrid.setAlignment(Pos.CENTER);

        TextField portField = new TextField("8080");
        portField.setPromptText("Port");
        portField.setPrefWidth(100);

        ComboBox<Integer> sizeBox = new ComboBox<>();
        sizeBox.getItems().addAll(9, 13, 19);
        sizeBox.setValue(19);
        sizeBox.setPrefWidth(100);

        configGrid.add(new Label("Port:"), 0, 0);
        configGrid.add(portField, 1, 0);
        configGrid.add(new Label("Rozmiar:"), 0, 1);
        configGrid.add(sizeBox, 1, 1);

        Label hostLabel = new Label("--- Stwórz Grę (Host) ---");
        hostLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        Button btnVsBot = new Button("Graj z BOTEM (Singleplayer)");
        styleButton(btnVsBot, "#4CAF50");
        btnVsBot.setOnAction(e -> {
            int port = parsePort(portField.getText());
            if (port > 0) startLocalGame(port, sizeBox.getValue(), true);
        });

        Button btnHostPvP = new Button("Graj z LUDŹMI (Multiplayer)");
        styleButton(btnHostPvP, "#2196F3");
        btnHostPvP.setOnAction(e -> {
            int port = parsePort(portField.getText());
            if (port > 0) startLocalGame(port, sizeBox.getValue(), false);
        });

        Label joinLabel = new Label("--- Dołącz do Gry (Klient) ---");
        joinLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        HBox joinBox = new HBox(10);
        joinBox.setAlignment(Pos.CENTER);
        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Adres IP");

        Button btnJoin = new Button("Połącz");
        styleButton(btnJoin, "#FF9800");
        btnJoin.setOnAction(e -> {
            int port = parsePort(portField.getText());
            if (port > 0) connectToServer(ipField.getText(), port);
        });
        joinBox.getChildren().addAll(ipField, btnJoin);

        root.getChildren().addAll(titleLabel, configGrid, hostLabel, btnVsBot, btnHostPvP, joinLabel, joinBox);

        Scene scene = new Scene(root, 450, 650);
        primaryStage.setTitle("Go Game - Menu");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    private void startLocalGame(int port, int size, boolean withBot) {
        try {
            GoServer.getInstance().stop();
            GoServer.getInstance().start(port, size, withBot);
            Thread.sleep(200);
            connectToServer("localhost", port);
        } catch (Exception e) {
            alertView.showAlert("Błąd", "Nie udało się uruchomić serwera: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void connectToServer(String ip, int port) {
        try {
            if (client == null) client = new GoClient(this);
            client.connect(ip, port);
            initGameView();
        } catch (IOException e) {
            alertView.showAlert("Błąd połączenia", "Nie można połączyć z " + ip + ":" + port, Alert.AlertType.ERROR);
        }
    }

    private void initGameView() {
        gameRoot = new BorderPane();
        gameRoot.setStyle("-fx-background-color: #333333;");

        VBox sidePanel = new VBox(10);
        sidePanel.setPadding(new Insets(10));
        sidePanel.setStyle("-fx-background-color: #DDDDDD;");
        sidePanel.setPrefWidth(250);
        sidePanel.setMinWidth(250);

        messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(400);

        Button btnPass = new Button("PAS");
        btnPass.setMaxWidth(Double.MAX_VALUE);
        btnPass.setOnAction(e -> client.sendPass());

        Button btnSurrender = new Button("PODDAJ SIĘ");
        btnSurrender.setMaxWidth(Double.MAX_VALUE);
        btnSurrender.setOnAction(e -> client.sendSurrender());

        Button btnResume = new Button("WZNÓW GRĘ");
        btnResume.setMaxWidth(Double.MAX_VALUE);
        btnResume.setOnAction(e -> client.sendResume());

        sidePanel.getChildren().addAll(new Label("Log gry:"), messageArea, btnPass, btnSurrender, btnResume, new Separator());
        gameRoot.setRight(sidePanel);

        Label loading = new Label("Oczekiwanie na dane planszy...");
        loading.setTextFill(Color.WHITE);
        gameRoot.setCenter(loading);

        Scene gameScene = new Scene(gameRoot, 1100, 800);
        primaryStage.setTitle("Go Game - Rozgrywka");
        primaryStage.setScene(gameScene);
        primaryStage.centerOnScreen();
    }

    public void updateBoard(String boardData) {
        Platform.runLater(() -> {
            String[] fields = boardData.split(";");
            int totalFields = fields.length;
            int calculatedSize = (int) Math.sqrt(totalFields);

            if (calculatedSize != currentBoardSize || boardPane == null) {
                currentBoardSize = calculatedSize;
                createBoardView(currentBoardSize);

                StackPane boardContainer = new StackPane(boardPane);
                boardContainer.setPadding(new Insets(20));
                boardContainer.setStyle("-fx-background-color: #333333;");

                ScrollPane scrollPane = new ScrollPane(boardContainer);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.setStyle("-fx-background: #333333; -fx-border-color: #333333;");

                gameRoot.setCenter(scrollPane);
            }

            for (int i = 0; i < totalFields; i++) {
                int x = i % currentBoardSize;
                int y = i / currentBoardSize;

                String field = fields[i];
                Circle stone = stones[x][y];

                if ("BLACK".equals(field)) {
                    stone.setFill(Color.BLACK);
                    stone.setStroke(Color.BLACK);
                } else if ("WHITE".equals(field)) {
                    stone.setFill(Color.WHITE);
                    stone.setStroke(Color.BLACK);
                } else {
                    stone.setFill(Color.TRANSPARENT);
                    stone.setStroke(Color.TRANSPARENT);
                }
            }
        });
    }

    private void createBoardView(int size) {
        boardPane = new Pane();

        int boardPixelSize = (size + 1) * CELL_SIZE;

        boardPane.setPrefSize(boardPixelSize, boardPixelSize);
        boardPane.setMinSize(boardPixelSize, boardPixelSize);
        boardPane.setStyle("-fx-background-color: #DCB35C; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");

        stones = new Circle[size][size];

        for (int i = 0; i < size; i++) {
            double offset = CELL_SIZE;

            Line vLine = new Line(
                    offset + i * CELL_SIZE, offset,
                    offset + i * CELL_SIZE, offset + (size - 1) * CELL_SIZE
            );
            Line hLine = new Line(
                    offset, offset + i * CELL_SIZE,
                    offset + (size - 1) * CELL_SIZE, offset + i * CELL_SIZE
            );
            boardPane.getChildren().addAll(vLine, hLine);
        }

        drawHoshi(size);

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Circle stone = new Circle(CELL_SIZE / 2.2);
                stone.setCenterX(CELL_SIZE + x * CELL_SIZE);
                stone.setCenterY(CELL_SIZE + y * CELL_SIZE);
                stone.setFill(Color.TRANSPARENT);
                stone.setStroke(Color.TRANSPARENT);

                final int finalX = x;
                final int finalY = y;
                stone.setOnMouseClicked(e -> client.sendMove(finalX, finalY));

                stone.setOnMouseEntered(e -> {
                    if (stone.getFill() == Color.TRANSPARENT) {
                        stone.setStroke(Color.GRAY);
                        stone.setStrokeWidth(2);
                    }
                });
                stone.setOnMouseExited(e -> {
                    if (stone.getFill() == Color.TRANSPARENT) {
                        stone.setStroke(Color.TRANSPARENT);
                    }
                });

                stones[x][y] = stone;
                boardPane.getChildren().add(stone);
            }
        }
    }

    private void drawHoshi(int size) {
        if (size == 19 || size == 13 || size == 9) {
            int[] points;
            if (size == 19) points = new int[]{3, 9, 15};
            else if (size == 13) points = new int[]{3, 6, 9};
            else points = new int[]{2, 6};

            for (int i : points) {
                for (int j : points) {
                    Circle dot = new Circle(CELL_SIZE + i * CELL_SIZE, CELL_SIZE + j * CELL_SIZE, 3);
                    dot.setFill(Color.BLACK);
                    boardPane.getChildren().add(dot);
                }
            }
        }
    }

    public void appendLog(String message) {
        Platform.runLater(() -> {
            messageArea.appendText(message + "\n");
            messageArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private int parsePort(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            alertView.showAlert("Błąd", "Nieprawidłowy numer portu.", Alert.AlertType.ERROR);
            return -1;
        }
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
        btn.setPrefWidth(220);
        btn.setPrefHeight(40);
    }
}