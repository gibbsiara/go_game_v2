package com.example;
/**
 * Core engine class managing the logic and flow of a Go game session.
 * Handles turn synchronization, move processing, and communication between players.
 */
public class Game {
    private Board board;
    private ClientHandler playerBlack;
    private ClientHandler playerWhite;
    private StoneColor currentPlayer;
    private RuleEngine ruleEngine;
    private boolean previousPlayerPassed = false;
    private boolean isGameOver = false;
    
    private int blackPrisoners = 0;
    private int whitePrisoners = 0;
    /**
     * Creates a new game session with a board of the specified size.
     * @param size The size of the board.
     */
    public Game(int size) {
        this.board = new Board(size);
        this.currentPlayer = StoneColor.BLACK;
        this.ruleEngine = new RuleEngine();
    }
    /**
     * Assigns players to the game session (first Black, then White).
     * @param player The ClientHandler representing a connected player.
     */
    public synchronized void addPlayer(ClientHandler player) {
        if (playerBlack == null) {
            playerBlack = player;
        } else if (playerWhite == null) {
            playerWhite = player;
        }
        ruleEngine.setPlayers(playerBlack, playerWhite);
    }
    /**
     * Processes a move attempt by a player. Handles regular moves or
     * dead stone removal if the game is over.
     * @param x The X-coordinate of the move.
     * @param y The Y-coordinate of the move.
     * @param playerColor The color of the player attempting the action.
     */
    public synchronized void processMove(int x, int y, StoneColor playerColor) {
        if (isGameOver) {
            if (board.getStone(x, y) != StoneColor.EMPTY) {
                if (board.getStone(x, y) == StoneColor.BLACK) {
                    whitePrisoners++;
                } else {
                    blackPrisoners++;
                }

                board.setStone(x, y, StoneColor.EMPTY);
        
                broadcastBoard();

                String scoreSummary = ruleEngine.calculateScore(board, blackPrisoners, whitePrisoners);
                broadcastMessage("MESSAGE AKTUALIZACJA WYNIKU (usunięto martwy kamień):\n" + scoreSummary);
            }
            return;
        }
        if (playerColor != currentPlayer) {
            notifyPlayer(playerColor, "MESSAGE To nie Twój ruch!");
            return;
        }

        if (!(ruleEngine.isMoveValid(board, x, y, playerColor))){
            return;
        }

        previousPlayerPassed = false;
        
        int captured = ruleEngine.playMove(board, x, y, playerColor);
        if (playerColor == StoneColor.BLACK) {
            blackPrisoners += captured;
        } else {
            whitePrisoners += captured;
        }

        switchTurn();
        broadcastBoard();
    }
    /**
     * Processes a "Pass" action. Ends the game if two players pass consecutively.
     * * @param playerColor The color of the player passing their turn.
     */
    public synchronized void processPass(StoneColor playerColor) {
        if (isGameOver) return;
        if (playerColor != currentPlayer) {
            notifyPlayer(playerColor, "MESSAGE To nie Twój ruch!");
            return;
        }

        notifyPlayer(StoneColor.BLACK, "MESSAGE Gracz " + playerColor + " spasował.");
        notifyPlayer(StoneColor.WHITE, "MESSAGE Gracz " + playerColor + " spasował.");

        if (previousPlayerPassed) {
            finishGameByPassing();
        } else {
            previousPlayerPassed = true;
            switchTurn();
            broadcastBoard();
        }
    }
    /**
     * Processes a player's surrender and declares the opponent as the winner.
     * * @param playerColor The color of the player who surrendered.
     */
    public synchronized void processSurrender(StoneColor playerColor) {
        if (isGameOver) return;
        StoneColor winner = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
        
        String msg = "MESSAGE KONIEC GRY: Gracz " + playerColor + " poddał się. Wygrywa " + winner + "!";
        broadcastMessage(msg);
        isGameOver = true;
    }

    private void finishGameByPassing() {
        isGameOver = true;
        
        String scoreSummary = ruleEngine.calculateScore(board, blackPrisoners, whitePrisoners);
        
        String msg = "MESSAGE KONIEC GRY (2 pasy).\nWYNIKI:\n" + scoreSummary;
        broadcastMessage(msg);
    }
    /**
     * Resumes the game from a finished state (dispute mode for dead stones).
     * * @param playerColor The color of the player requesting the resume.
     */
    public synchronized void processResume(StoneColor playerColor) {
        if (!isGameOver) {
             notifyPlayer(playerColor, "MESSAGE Gra wciąż trwa, nie można wznowić.");
             return;
        }

        isGameOver = false;
        previousPlayerPassed = false;

        currentPlayer = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;

        String msg = "MESSAGE Gra została wznowiona przez " + playerColor + ". Ruch ma teraz " + currentPlayer;
        notifyPlayer(StoneColor.BLACK, msg);
        notifyPlayer(StoneColor.WHITE, msg);
        
        broadcastBoard();
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
    }
    /**
     * Sends the current board state to both players.
     * The board is serialized into a string format (BOARD followed by stone colors).
     * It also notifies whose turn it is and the current prisoner count.
     */
    private void broadcastBoard() {
        StringBuilder sb = new StringBuilder();

        int size = board.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                sb.append(board.getStone(x, y).toString());
                sb.append(";");
            }
        }
        String boardData = sb.toString();
        String message = "BOARD " + boardData;
    
        if (playerBlack != null) playerBlack.sendMessage(message);
        if (playerWhite != null) playerWhite.sendMessage(message);
    
        if (!isGameOver) {
            String prisonersMsg = " (Jeńcy: B=" + blackPrisoners + ", W=" + whitePrisoners + ")";
            if (currentPlayer == StoneColor.BLACK) {
                if (playerBlack != null) playerBlack.sendMessage("MESSAGE Wykonaj ruch [X]" + prisonersMsg);
                if (playerWhite != null) playerWhite.sendMessage("MESSAGE Oczekiwanie na ruch przeciwnika..." + prisonersMsg);
            } else {
                if (playerWhite != null) playerWhite.sendMessage("MESSAGE Wykonaj ruch [O]" + prisonersMsg);
                if (playerBlack != null) playerBlack.sendMessage("MESSAGE Oczekiwanie na ruch przeciwnika..." + prisonersMsg);
            }
        }
    }
    /**
     * Sends a text message to both players' consoles/chat areas.
     * @param msg The message string to be sent.
     */
    private void broadcastMessage(String msg) {
        if (playerBlack != null) playerBlack.sendMessage(msg);
        if (playerWhite != null) playerWhite.sendMessage(msg);
    }
    /**
     * Sends a private message to a specific player based on their color.
     * @param color The color of the player to notify.
     * @param msg The message string to be sent.
     */
    private void notifyPlayer(StoneColor color, String msg) {
        if (color == StoneColor.BLACK && playerBlack != null) {
            playerBlack.sendMessage(msg);
        } else if (color == StoneColor.WHITE && playerWhite != null) {
            playerWhite.sendMessage(msg);
        }
    }
}