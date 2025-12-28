package com.example;

public class Game {
    private Board board;
    private ClientHandler playerBlack;
    private ClientHandler playerWhite;
    private StoneColor currentPlayer;
    private RuleEngine ruleEngine;

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
    }

    public synchronized void processMove(int x, int y, StoneColor playerColor) {
        if (!(ruleEngine.isMoveValid(board, x, y, playerColor))){
            notifyPlayer(playerColor, "MESSAGE Niedozwolony ruch");
            return;
        }

        ruleEngine.playMove(board, x, y, playerColor);
        switchTurn();
        broadcastBoard();
    }

    private void switchTurn() {
        currentPlayer = (currentPlayer == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
    }

    private void broadcastBoard() {
        String boardData = board.toString().replace('\n', ';');
        String message = "BOARD " + boardData;
    
        if (playerBlack != null) playerBlack.sendMessage(message);
        if (playerWhite != null) playerWhite.sendMessage(message);
    
        if (currentPlayer == StoneColor.BLACK) {
            if (playerBlack != null) {
                playerBlack.sendMessage("MESSAGE Wykonaj ruch [X]");
            }
            if (playerWhite != null) {
                playerWhite.sendMessage("MESSAGE Oczekiwanie na ruch przeciwnika...");
            }
        } else {
            if (playerWhite != null) {
                playerWhite.sendMessage("MESSAGE Wykonaj ruch [O]");
            }
            if (playerBlack != null) {
                playerBlack.sendMessage("MESSAGE Oczekiwanie na ruch przeciwnika...");
            }
        }
    }

    private void notifyPlayer(StoneColor color, String msg) {
        if (color == StoneColor.BLACK && playerBlack != null) {
            playerBlack.sendMessage(msg);
        } else if (color == StoneColor.WHITE && playerWhite != null) {
            playerWhite.sendMessage(msg);
        }
    }
}