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

    private GameService gameService;
    private Long dbGameId;
    private int moveCounter = 0;
    private boolean persistenceEnabled = true;

    public Game(int size) {
        this.board = new Board(size);
        this.currentPlayer = StoneColor.BLACK;
        this.ruleEngine = new RuleEngine();
    }

    public void setGameService(GameService service) {
        this.gameService = service;
        if (this.gameService != null && persistenceEnabled) {
            this.dbGameId = this.gameService.createNewGame(board.getSize());
        }
    }

    public void setPersistenceEnabled(boolean enabled) {
        this.persistenceEnabled = enabled;
    }

    public synchronized void addPlayer(Player player) {
        if (playerBlack == null) {
            playerBlack = player;
        } else if (playerWhite == null) {
            playerWhite = player;
        }
        ruleEngine.setPlayers(playerBlack, playerWhite);
    }

    public synchronized void processMove(int x, int y, StoneColor playerColor) {
        if (isGameOver) {
            notifyPlayer(playerColor, "MESSAGE Gra zakończona. Nie można wykonywać ruchów.");
            return;
        }

        if (persistenceEnabled && playerColor != currentPlayer) {
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

            if (persistenceEnabled && gameService != null && dbGameId != null) {
                moveCounter++;
                gameService.saveMove(dbGameId, moveCounter, x, y, playerColor, "MOVE");
            }

            previousPlayerPassed = false;
            switchTurn();
            broadcastState();
        }
    }

    public synchronized void processPass(StoneColor playerColor) {
        if (isGameOver) return;

        if (persistenceEnabled && playerColor != currentPlayer) {
            notifyPlayer(playerColor, "MESSAGE To nie jest Twój ruch!");
            return;
        }

        if (persistenceEnabled && gameService != null && dbGameId != null) {
            moveCounter++;
            gameService.saveMove(dbGameId, moveCounter, -1, -1, playerColor, "PASS");
        }

        if (previousPlayerPassed) {
            endGame();
        } else {
            previousPlayerPassed = true;
            switchTurn();
            broadcastMessage("MESSAGE Tura gracza: " + currentPlayer);
        }
        notifyPlayer(playerColor, "MESSAGE Spasowałeś.");
        notifyPlayer(getOpponent(playerColor), "MESSAGE Przeciwnik spasował.");
    }

    public synchronized void processSurrender(StoneColor playerColor) {
        if (isGameOver) return;
        isGameOver = true;
        StoneColor winner = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;

        if (persistenceEnabled && gameService != null && dbGameId != null) {
             moveCounter++;
             gameService.saveMove(dbGameId, moveCounter, -1, -1, playerColor, "SURRENDER");
             gameService.finishGame(dbGameId, winner + "_WON");
        }

        broadcastMessage("MESSAGE Gracz " + playerColor + " poddał się. Wygrywa " + winner + "!");
    }

    public synchronized void processResume(StoneColor playerColor) {
    }

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

        String result;
        if (finalBlackScore > finalWhiteScore) {
            sb.append("Wygrywa CZARNY!");
            result = "BLACK_WON";
        } else if (finalWhiteScore > finalBlackScore) {
            sb.append("Wygrywa BIAŁY!");
            result = "WHITE_WON";
        } else {
            sb.append("REMIS!");
            result = "DRAW";
        }

        if (persistenceEnabled && gameService != null && dbGameId != null) {
            gameService.finishGame(dbGameId, result);
        }

        broadcastMessage(sb.toString());
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
    }

    private StoneColor getOpponent(StoneColor color) {
        return (color == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
    }

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

    public synchronized Board getBoard() {
        return board;
    }
}