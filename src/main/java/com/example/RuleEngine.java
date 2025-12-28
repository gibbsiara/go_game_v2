package com.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuleEngine {
    private Board board;
    private StoneColor currentPlayerColor = StoneColor.BLACK;
    private ClientHandler playerBlack;
    private ClientHandler playerWhite;

    public boolean isMoveValid(Board board, int x, int y, StoneColor playerColor) {
        if (currentPlayerColor != playerColor) {
            notifyPlayer(playerColor, "MESSAGE: To nie Twój ruch!");
            return false;
        }
        if (x < 0 || x >= board.getSize() || y < 0 || y >= board.getSize()) {
            notifyPlayer(playerColor, "MESSAGE: Ruch poza planszą!");
            return false;
        }
        if (board.getStone(x, y) != StoneColor.EMPTY) {
            notifyPlayer(playerColor, "MESSAGE: Pole jest zajęte!");
            return false;
        }

        StoneColor originalColor = board.getStone(x, y);
        board.setStone(x, y, playerColor);

        boolean capturesOpponent = checkAndProcessCaptures(board, x, y, playerColor, false);

        int selfLiberties = countLiberties(board, x, y);

        board.setStone(x, y, originalColor);

        if (selfLiberties == 0 && !capturesOpponent) {
            notifyPlayer(playerColor, "MESSAGE: Ruch samobójczy jest zabroniony!");
            return false;
        }

        return true;
    }

    public void playMove(Board board, int x, int y, StoneColor playerColor) {
        board.setStone(x, y, playerColor);
        checkAndProcessCaptures(board, x, y, playerColor, true);
        currentPlayerColor = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
    }

    private boolean checkAndProcessCaptures(Board board, int x, int y, StoneColor playerColor, boolean actuallyRemove) {
        StoneColor opponentColor = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};
        boolean capturedAnything = false;

        for (int[] n : neighbors) {
            int nx = n[0], ny = n[1];
            if (isOnBoard(board, nx, ny) && board.getStone(nx, ny) == opponentColor) {
                if (countLiberties(board, nx, ny) == 0) {
                    if (actuallyRemove) {
                        removeGroup(board, nx, ny);
                    }
                    capturedAnything = true;
                }
            }
        }
        return capturedAnything;
    }

    private int countLiberties(Board board, int x, int y) {
        Set<String> visited = new HashSet<>();
        Set<String> liberties = new HashSet<>();
        findGroupAndLiberties(board, x, y, board.getStone(x, y), visited, liberties);
        return liberties.size();
    }

    private void findGroupAndLiberties(Board board, int x, int y, StoneColor color, Set<String> visited, Set<String> liberties) {
        String pos = x + "," + y;
        if (visited.contains(pos) || !isOnBoard(board, x, y)) return;

        visited.add(pos);
        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};

        for (int[] n : neighbors) {
            int nx = n[0], ny = n[1];
            if (isOnBoard(board, nx, ny)) {
                StoneColor neighborStone = board.getStone(nx, ny);
                if (neighborStone == StoneColor.EMPTY) {
                    liberties.add(nx + "," + ny);
                } else if (neighborStone == color) {
                    findGroupAndLiberties(board, nx, ny, color, visited, liberties);
                }
            }
        }
    }

    private void removeGroup(Board board, int x, int y) {
        StoneColor color = board.getStone(x, y);
        if (color == StoneColor.EMPTY) return;

        board.setStone(x, y, StoneColor.EMPTY);
        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};
        for (int[] n : neighbors) {
            if (isOnBoard(board, n[0], n[1]) && board.getStone(n[0], n[1]) == color) {
                removeGroup(board, n[0], n[1]);
            }
        }
    }

    private boolean isOnBoard(Board board, int x, int y) {
        return x >= 0 && x < board.getSize() && y >= 0 && y < board.getSize();
    }

    private void notifyPlayer(StoneColor color, String msg) {
        if (color == StoneColor.BLACK && playerBlack != null) {
            playerBlack.sendMessage(msg);
        } else if (color == StoneColor.WHITE && playerWhite != null) {
            playerWhite.sendMessage(msg);
        }
    }
}