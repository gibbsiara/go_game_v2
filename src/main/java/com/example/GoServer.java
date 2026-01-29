package com.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class GoServer {
    private static GoServer instance;
    private ServerSocket serverSocket;
    private boolean isRunning;

    private GoServer() {}

    public static GoServer getInstance() {
        if (instance == null) {
            instance = new GoServer();
        }
        return instance;
    }

    /**
     * Startuje serwer.
     * @param port Port serwera
     * @param size Rozmiar planszy
     * @param playWithBot TRUE = gra z botem, FALSE = czekaj na drugiego gracza
     */
    public void start(int port, int size, boolean playWithBot) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                System.out.println("SERWER START: Port " + port + ", Rozmiar " + size);
                System.out.println("TRYB: " + (playWithBot ? "Człowiek vs Bot" : "Multiplayer"));

                Game game = new Game(size);

                System.out.println("Oczekiwanie na Gracza 1 (Czarne)...");
                Socket socket1 = serverSocket.accept();
                System.out.println("Gracz 1 połączony.");

                ClientHandler player1 = new ClientHandler(socket1, game, StoneColor.BLACK);
                game.addPlayer(player1);
                new Thread(player1).start();

                if (playWithBot) {
                    System.out.println("Dodawanie Bota (Białe)...");
                    BotPlayer bot = new BotPlayer(game, StoneColor.WHITE);
                    game.addPlayer(bot);
                    new Thread(bot).start();
                } else {
                    System.out.println("Oczekiwanie na Gracza 2 (Białe)...");
                    Socket socket2 = serverSocket.accept();
                    System.out.println("Gracz 2 połączony.");

                    ClientHandler player2 = new ClientHandler(socket2, game, StoneColor.WHITE);
                    game.addPlayer(player2);
                    new Thread(player2).start();
                }

            } catch (IOException e) {
                System.err.println("Błąd serwera: " + e.getMessage());
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}