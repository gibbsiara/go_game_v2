package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles the server-side connection for a single player.
 * Runs on a separate thread to receive and process messages from the client.
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Game game;
    private StoneColor color;

    /**
     * Initializes the client handler with the socket and game reference.
     * @param socket The connected socket for the player.
     * @param game The main game instance.
     * @param color The color assigned to this player.
     */
    public ClientHandler(Socket socket, Game game, StoneColor color) {
        this.socket = socket;
        this.game = game;
        this.color = color;
    }


    /**
     * The main execution loop for the client thread.
     * Listens for commands (MOVE, PASS, etc.) and executes them via the Command pattern.
     */
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
                } else if (inputLine.startsWith("PASS")) {
                    Command command = new PassCommand(game, color);
                    command.execute();
                } else if (inputLine.startsWith("SURRENDER")) {
                    Command command = new SurrenderCommand(game, color);
                    command.execute();
                } else if (inputLine.startsWith("RESUME")) {
                    Command command = new ResumeCommand(game, color);
                    command.execute();
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

    /**
     * Initializes input and output streams for communication.
     * Sends the assigned color to the client immediately upon connection.
     * @throws IOException If stream creation fails.
     */
    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        sendMessage("Twój kolor to: " + color);
    }

    /**
     * Reads a single line of text from the client.
     * @return The message string sent by the client.
     * @throws IOException If reading from the stream fails.
     */
    public String receiveMessage() throws IOException {
        return in.readLine();
    }

    /**
     * Sends a raw string message to the client.
     * @param message The text to send.
     */
    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Parses a move command string and executes the corresponding logic.
     * Expected format: "MOVE x y".
     * @param inputLine The raw command string received from the client.
     */
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

    /**
     * Closes the socket connection safely.
     */
    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}