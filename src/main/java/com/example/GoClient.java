package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GoClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ConsoleView view;

    public GoClient(ConsoleView view) {
        this.view = view;
    }

    public void connect(String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        new Thread(this::listenForServer).start();
    }

    public void listenForServer() {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                if (response.startsWith("BOARD")) {
                    String boardData = response.substring(6);
                    view.displayBoard(boardData);
                } 
                else if (response.startsWith("MESSAGE")) {
                    view.displayMessage(response.substring(8));
                }
                else {
                    view.displayMessage(response);
                }
            }
        } catch (IOException e) {
            System.out.println("Utracono połączenie z serwerem.");
            try { socket.close(); } catch (IOException ignored) {}
            System.exit(0);
        }
    }

    public void sendMove(int x, int y) {
        out.println("MOVE " + x + " " + y);
    }

    public void sendCommand(String command) {
        out.println(command);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConsoleView view = new ConsoleView(scanner);
        GoClient client = new GoClient(view);
        try {
            view.displayMessage("Wpisz port, na którym odbędzie się gra.");
            
            int GetUserPort;
            try {
                 String portStr = view.getUserInput();
                 GetUserPort = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                 GetUserPort = 12345; 
                 view.displayMessage("Błędny port, używam domyślnego: 12345");
            }

            client.connect("localhost", GetUserPort);

            view.displayMessage("Połączono! Oczekiwanie na grę...");
            view.displayMessage("Aby wykonać ruch, wpisz: wiersz kolumna (indeksy od 0)");
            view.displayMessage("Aby wyjść, wpisz: quit");

            while (true) {
                String input = view.getUserInput();

                if (input == null || input.equalsIgnoreCase("quit")) {
                    client.sendCommand("QUIT");
                    break;
                }

                String[] parts = input.trim().split("\\s+");
                if (parts.length == 2) {
                    try {
                        int row = Integer.parseInt(parts[0]);
                        int col = Integer.parseInt(parts[1]);
                        
                        client.sendMove(col, row);
                    } catch (NumberFormatException e) {
                        System.out.println("Współrzędne muszą być liczbami!");
                    }
                } else {
                    System.out.println("Niepoprawny format. Użyj: wiersz kolumna");
                }
            }
        } catch (IOException e) {
            System.out.println("Nie udało się połączyć z serwerem");
        }
    }
}