package com.example;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GoClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private GoApplication gui;

    public GoClient(GoApplication gui) {
        this.gui = gui;
    }

    public void connect(String ip, int port) throws IOException {
        this.socket = new Socket(ip, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        new Thread(this::listenForServer).start();
    }

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

    public void sendMove(int x, int y) {
        if (out != null) {
            out.println("MOVE " + x + " " + y);
        }
    }

    public void sendQuit() {
        if (out != null) {
            out.println("QUIT");
        }
    }
}