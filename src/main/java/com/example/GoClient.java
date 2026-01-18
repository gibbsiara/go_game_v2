package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javafx.application.Platform;
/**
 * Handles network communication on the client side.
 * Runs a background thread to listen for server updates and redirects them to the GUI.
 */
public class GoClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GoApplication gui;

    public GoClient(GoApplication gui) {
        this.gui = gui;
    }
    /**
     * Establishes a socket connection to the game server.
     * @param ip Server IP address.
     * @param port Server port number.
     * @throws IOException If the connection fails.
     */
    public void connect(String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        new Thread(this::listenForServer).start();
    }
    /**
     * A background loop that listens for incoming messages from the server.
     * It processes different types of server commands:
     * - BOARD: Triggers a graphical board update.
     * - MESSAGE: Appends a system or game message to the chat log.
     * Updates are wrapped in Platform.runLater to ensure thread safety with JavaFX.
     */
    private void listenForServer() {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                String finalResponse = response;

                Platform.runLater(() -> {
                    if (finalResponse.startsWith("BOARD")) {
                        String boardData = finalResponse.substring(6);
                        gui.updateBoard(boardData);
                    } else if (finalResponse.startsWith("MESSAGE")) {
                        gui.appendLog(finalResponse.substring(8));
                    } else {
                        gui.appendLog(finalResponse);
                    }
                });
            }
        } catch (IOException e) {
            Platform.runLater(() -> gui.appendLog("Rozłączono z serwerem."));
        }
    }
    /**
     * Sends a move request to the server with the specified coordinates.
     * The message is formatted as "MOVE x y".
     * @param x The X-coordinate on the board.
     * @param y The Y-coordinate on the board.
     */
    public void sendMove(int x, int y) {
        if (out != null) {
            out.println("MOVE " + x + " " + y);
        }
    }
    /**
     * Sends a "PASS" command to the server, indicating the player
     * chooses to skip their current turn.
     */
    public void sendPass() {
        if (out != null) {
            out.println("PASS");
        }
    }
    /**
     * Sends a "SURRENDER" command to the server, effectively
     * conceding the game to the opponent.
     */
    public void sendSurrender() {
        if (out != null) {
            out.println("SURRENDER");
        }
    }
    /**
     * Sends a "RESUME" command to the server.
     * This is typically used to exit the "Game Over" state if players
     * wish to continue playing (e.g., during a dispute over dead stones).
     */
    public void sendResume() {
        if (out != null) {
            out.println("RESUME");
        }
    }
    /**
     * Sends a "QUIT" command to the server to signal that the player
     * is leaving the game session.
     */
    public void sendQuit() {
        if (out != null) {
            out.println("QUIT");
        }
    }
}