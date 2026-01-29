package com.example;

/**
 * Core engine class managing the logic and flow of a Go game session.
 * Handles turn synchronization, move processing, score calculation and communication.
 */
public class Game {
    private Board board;
    private Player playerBlack;
    private Player playerWhite;

    private StoneColor currentPlayer;
    private RuleEngine ruleEngine;

    private boolean previousPlayerPassed = false;
    private boolean isGameOver = false;

    private int blackPrisoners = 0;
    private int whitePrisoners = 0;

    /**
     * Creates a new game session with a board of the specified size.
     */
    public Game(int size) {
        this.board = new Board(size);
        this.currentPlayer = StoneColor.BLACK;
        this.ruleEngine = new RuleEngine();
    }

    /**
     * Assigns players to the game session (first Black, then White).
     * Accepts any object implementing the Player interface (Human or Bot).
     */
    public synchronized void addPlayer(Player player) {
        if (playerBlack == null) {
            playerBlack = player;
            if (player instanceof ClientHandler) {
            }
        } else if (playerWhite == null) {
            playerWhite = player;
        }

        ruleEngine.setPlayers(playerBlack, playerWhite);
    }

    /**
     * Processes a move attempt by a player.
     */
    public synchronized void processMove(int x, int y, StoneColor playerColor) {
        if (isGameOver) {
            notifyPlayer(playerColor, "MESSAGE Gra zakończona. Nie można wykonywać ruchów.");
            return;
        }

        if (playerColor != currentPlayer) {
            notifyPlayer(playerColor, "MESSAGE To nie jest Twój ruch!");
            return;
        }

        if (ruleEngine.isMoveValid(board, x, y, playerColor)) {

            int captured = ruleEngine.getLastCapturedCount();
            if (playerColor == StoneColor.BLACK) {
                blackPrisoners += captured;
            } else {
                whitePrisoners += captured;
            }

            previousPlayerPassed = false;
            switchTurn();
            broadcastState();
        }
    }

    /**
     * Processes a PASS command. Two consecutive passes end the game.
     */
    public synchronized void processPass(StoneColor playerColor) {
        if (isGameOver) return;

        if (playerColor != currentPlayer) {
            notifyPlayer(playerColor, "MESSAGE To nie jest Twój ruch!");
            return;
        }

        notifyPlayer(playerColor, "MESSAGE Spasowałeś.");
        notifyPlayer(getOpponent(playerColor), "MESSAGE Przeciwnik spasował.");

        if (previousPlayerPassed) {
            endGame();
        } else {
            previousPlayerPassed = true;
            switchTurn();
            broadcastMessage("MESSAGE Tura gracza: " + currentPlayer);
        }
    }

    /**
     * Handles surrender.
     */
    public synchronized void processSurrender(StoneColor playerColor) {
        if (isGameOver) return;
        isGameOver = true;
        StoneColor winner = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
        broadcastMessage("MESSAGE Gracz " + playerColor + " poddał się. Wygrywa " + winner + "!");
    }

    /**
     * Calculates score and ends the game.
     */
    private void endGame() {
        isGameOver = true;

        int[] territories = ruleEngine.countTerritory(board);
        int blackTerritory = territories[0];
        int whiteTerritory = territories[1];

        double finalBlackScore = blackTerritory + blackPrisoners;
        double finalWhiteScore = whiteTerritory + whitePrisoners;

        StringBuilder sb = new StringBuilder();
        sb.append("MESSAGE === KONIEC GRY ===\n");
        sb.append("CZARNY: Teren=" + blackTerritory + ", Jeńcy=" + blackPrisoners + " -> SUMA: " + finalBlackScore + "\n");
        sb.append("BIAŁY:  Teren=" + whiteTerritory + ", Jeńcy=" + whitePrisoners + " -> SUMA: " + finalWhiteScore + "\n");

        if (finalBlackScore > finalWhiteScore) {
            sb.append("Wygrywa CZARNY!");
        } else if (finalWhiteScore > finalBlackScore) {
            sb.append("Wygrywa BIAŁY!");
        } else {
            sb.append("REMIS!");
        }

        broadcastMessage(sb.toString());
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
    }

    private StoneColor getOpponent(StoneColor color) {
        return (color == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
    }

    /**
     * Sends the current board state to all players.
     */
    private void broadcastState() {
        String state = board.getBoardStateString();
        String prisonersMsg = " (Jeńcy: B=" + blackPrisoners + ", W=" + whitePrisoners + ")";

        broadcastMessage("BOARD " + state);

        if (!isGameOver) {
            if (currentPlayer == StoneColor.BLACK) {
                notifyPlayer(StoneColor.BLACK, "MESSAGE Twój ruch" + prisonersMsg);
                notifyPlayer(StoneColor.WHITE, "MESSAGE Ruch przeciwnika..." + prisonersMsg);
            } else {
                notifyPlayer(StoneColor.WHITE, "MESSAGE Twój ruch" + prisonersMsg);
                notifyPlayer(StoneColor.BLACK, "MESSAGE Ruch przeciwnika..." + prisonersMsg);
            }
        }
    }

    private void broadcastMessage(String msg) {
        if (playerBlack != null) playerBlack.sendMessage(msg);
        if (playerWhite != null) playerWhite.sendMessage(msg);
    }

    private void notifyPlayer(StoneColor color, String msg) {
        if (color == StoneColor.BLACK && playerBlack != null) {
            playerBlack.sendMessage(msg);
        } else if (color == StoneColor.WHITE && playerWhite != null) {
            playerWhite.sendMessage(msg);
        }
    }

    public synchronized void processResume(StoneColor playerColor) {
    }
    public synchronized Board getBoard() {
        return board;
    }

}