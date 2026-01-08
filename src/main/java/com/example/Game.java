package com.example;

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

    public Game(int size) {
        this.board = new Board(size);
        this.currentPlayer = StoneColor.BLACK;
        this.ruleEngine = new RuleEngine();
    }

    public synchronized void addPlayer(ClientHandler player) {
        if (playerBlack == null) {
            playerBlack = player;
        } else if (playerWhite == null) {
            playerWhite = player;
        }
        ruleEngine.setPlayers(playerBlack, playerWhite);
    }

    public synchronized void processMove(int x, int y, StoneColor playerColor) {
        if (isGameOver) {
            notifyPlayer(playerColor, "MESSAGE Gra skończona. Nie można wykonywać ruchów.");
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
}