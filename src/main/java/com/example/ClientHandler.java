package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Game game;
    private StoneColor color;

    public ClientHandler(Socket socket, Game game, StoneColor color) {
        this.socket = socket;
        this.game = game;
        this.color = color;
    }


    @Override
    public void run() {
        try {
            setupStreams();
            while (true) {
                String inputLine = receiveMessage(); 
                
                if (inputLine == null) break;

                System.out.println("SERWER: Otrzymano od " + color + ": " + inputLine);

                if (inputLine.startsWith("MOVE")) {
                    processMoveCommand(inputLine);
                } else if (inputLine.startsWith("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Gracz " + color + " rozłączył się.");
        } finally {
            closeConnection();
        }
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        sendMessage("Twój kolor to: " + color);
    }

    public String receiveMessage() throws IOException {
        return in.readLine();
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void processMoveCommand(String inputLine) {
        try {
            String[] parts = inputLine.split(" ");
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);

            MoveCommand command = new MoveCommand(game, x, y, color);
            command.execute(); 

        } catch (Exception e) {
            sendMessage("MESSAGE Błąd przetwarzania ruchu: " + e.getMessage());
        }
    }

    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}