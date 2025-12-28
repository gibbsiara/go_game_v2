package com.example;

import javafx.scene.control.Alert;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public final class GoServer {
    private static GoServer instance;
    private Scanner scanner;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private GoServer() {
        this.scanner = new Scanner(System.in);
    }
    private AlertView alertView = new AlertView();
    public static GoServer getInstance() {
        if (instance == null) {
            instance = new GoServer();
        }
        return instance;
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            while (isRunning) {
                int boardSize = getValidBoardSize();
                Game game = new Game(boardSize);
                alertView.showAlert("Go Server", "Oczekiwanie na graczy...", Alert.AlertType.INFORMATION);

                Socket socket1 = serverSocket.accept();

                alertView.showAlert("Go Server", "Połączono gracza 1 [BLACK]", Alert.AlertType.INFORMATION);

                ClientHandler player1 = new ClientHandler(socket1, game, StoneColor.BLACK);
                game.addPlayer(player1);

                Socket socket2 = serverSocket.accept();
                alertView.showAlert("Go Server","Połączono gracza 1 [BLACK]", Alert.AlertType.INFORMATION );

                ClientHandler player2 = new ClientHandler(socket2, game, StoneColor.WHITE);
                game.addPlayer(player2);

                Thread t1 = new Thread(player1);
                Thread t2 = new Thread(player2);

                t1.start();
                t2.start();

                alertView.showAlert("Go Server", "Gra rozpoczęta", Alert.AlertType.INFORMATION);

                try {
                    t1.join();
                    t2.join();
                } catch (InterruptedException e) {
                    alertView.showAlert("Go Server", "Serwer przerwany podczas gry", Alert.AlertType.ERROR);
                    Thread.currentThread().interrupt();
                }
                alertView.showAlert("Go Server", "Gra zakończyła się", Alert.AlertType.INFORMATION);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getValidBoardSize() {
        int size = 0;

        while (true) {
            System.out.println("Podaj wielkość planszy: 9x9, 13x13, 19x19");
            try {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) continue; 
                
                size = Integer.parseInt(line);

                if (size == 9 || size == 13 || size == 19) {
                    break;
                } else {
                    System.out.println("Niewłaściwy rozmiar planszy. Dostępne: 9, 13 lub 19");
                }

            } catch (NumberFormatException e) {
                System.out.println("Input nie jest poprawnym integerem!");
            }
        }

        return size;
    }
}