package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable, Player {
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
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            sendMessage("Twój kolor to: " + color);
            sendMessage("BOARD " + game.getBoard().getBoardStateString());

            while (true) {
                String inputLine = in.readLine();
                if (inputLine == null) break;

                System.out.println("SERWER (" + color + "): " + inputLine);

                if (inputLine.startsWith("MOVE")) {
                    String[] parts = inputLine.split(" ");
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    new MoveCommand(game, x, y, color).execute();

                } else if (inputLine.startsWith("PASS")) {
                    new PassCommand(game, color).execute();
                } else if (inputLine.startsWith("SURRENDER")) {
                    new SurrenderCommand(game, color).execute();
                } else if (inputLine.startsWith("RESUME")) {
                    new ResumeCommand(game, color).execute();
                } else if (inputLine.startsWith("REPLAY")) {
                    try {
                        String[] parts = inputLine.split(" ");
                        Long gameId = Long.parseLong(parts[1]);
                        
                        GameService service = GoServer.getInstance().getGameService();
                        if (service != null) {
                            service.playReplayForClient(gameId, this);
                        } else {
                            sendMessage("MESSAGE Błąd: Serwis gry niedostępny.");
                        }
                    } catch (Exception e) {
                        sendMessage("MESSAGE Błąd komendy replay: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Gracz " + color + " rozłączył się.");
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }

    @Override
    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public StoneColor getColor() {
        return color;
    }
}