package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@SpringBootApplication
public class GoServer {
    private static GoServer instance;
    private ConfigurableApplicationContext springContext;
    private ServerSocket serverSocket;
    private boolean isRunning;
    private GameService gameService;

    public GoServer() {}

    public static GoServer getInstance() {
        if (instance == null) {
            instance = new GoServer();
        }
        return instance;
    }

    public void start(int port, int size, boolean playWithBot) {
        if (springContext == null) {
            springContext = SpringApplication.run(GoServer.class);
        }
        this.gameService = springContext.getBean(GameService.class);

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                System.out.println("SERWER START: Port " + port + ", Rozmiar " + size);
                
                Game game = new Game(size);
                game.setGameService(gameService);

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

                System.out.println("Gra rozpoczęta. Serwer nasłuchuje teraz widzów/replay...");

                while (isRunning) {
                    try {
                        Socket spectatorSocket = serverSocket.accept();
                        System.out.println("Nowe połączenie (Widz/Replay).");
                        
                        ClientHandler spectator = new ClientHandler(spectatorSocket, game, StoneColor.EMPTY);
                        new Thread(spectator).start();
                        
                    } catch (IOException e) {
                        if (isRunning) System.err.println("Błąd połączenia z widzem: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
                System.err.println("Krytyczny błąd serwera: " + e.getMessage());
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

    public GameService getGameService() {
        return gameService;
    }
}