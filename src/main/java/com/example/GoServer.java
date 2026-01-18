package com.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Singleton server class that manages the game session lifecycle.
 * Responsible for accepting player connections and initializing the game logic.
 */
public final class GoServer {
    private static GoServer instance;
    private ServerSocket serverSocket;
    private boolean isRunning;

    private GoServer() {}

    /**
     * Retrieves the single instance of the GoServer.
     * @return The singleton GoServer instance.
     */
    public static GoServer getInstance() {
        if (instance == null) {
            instance = new GoServer();
        }
        return instance;
    }

    /**
     * Starts the server on a specified port and board size.
     * Runs on a separate thread to accept two players and start the game loop.
     * @param port The port number to listen on.
     * @param size The size of the game board.
     */
    public void start(int port, int size) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                System.out.println("Serwer wystartował na porcie: " + port + ", rozmiar: " + size);

                Game game = new Game(size);

                Socket socket1 = serverSocket.accept();
                System.out.println("Gracz 1 dołączył.");
                ClientHandler player1 = new ClientHandler(socket1, game, StoneColor.BLACK);
                game.addPlayer(player1);

                Socket socket2 = serverSocket.accept();
                System.out.println("Gracz 2 dołączył.");
                ClientHandler player2 = new ClientHandler(socket2, game, StoneColor.WHITE);
                game.addPlayer(player2);

                Thread t1 = new Thread(player1);
                Thread t2 = new Thread(player2);
                t1.start();
                t2.start();

                try {
                    t1.join();
                    t2.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Stops the server and closes the active socket connection.
     */
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