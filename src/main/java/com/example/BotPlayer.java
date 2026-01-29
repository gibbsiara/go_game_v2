package com.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BotPlayer implements Player, Runnable {
    private Game game;
    private StoneColor myColor;
    private StoneColor opponentColor;

    private Set<String> failedMovesInThisTurn;
    private int lastX = -1;
    private int lastY = -1;

    public BotPlayer(Game game, StoneColor color) {
        this.game = game;
        this.myColor = color;
        this.opponentColor = (color == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
        this.failedMovesInThisTurn = new HashSet<>();
    }

    @Override
    public void sendMessage(String msg) {
        if (msg.contains("Twój ruch")) {
            failedMovesInThisTurn.clear();
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            makeBestMove();
        }
        else if (msg.toLowerCase().contains("błąd") || msg.toLowerCase().contains("niedozwolony")) {
            System.out.println("BOT: Serwer odrzucił ruch (" + lastX + "," + lastY + "). Próbuję inny...");

            if (lastX != -1 && lastY != -1) {
                failedMovesInThisTurn.add(lastX + "," + lastY);
            }
            makeBestMove();
        }
    }

    @Override
    public void run() {
        System.out.println("Bot Strategiczny (" + myColor + ") gotowy.");
    }

    /**
     * Główna logika: Ocenia każde pole i wybiera to z najwyższym wynikiem.
     */
    private void makeBestMove() {
        Board board = game.getBoard();
        int size = board.getSize();

        double bestScore = -Double.MAX_VALUE;
        int[] bestMove = null;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board.getStone(x, y) != StoneColor.EMPTY || failedMovesInThisTurn.contains(x + "," + y)) {
                    continue;
                }

                double score = evaluateMove(board, x, y);

                score += Math.random();

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = new int[]{x, y};
                }
            }
        }

        if (bestMove != null) {
            if (bestScore < -500) {
                game.processPass(myColor);
            } else {
                lastX = bestMove[0];
                lastY = bestMove[1];
                game.processMove(lastX, lastY, myColor);
            }
        } else {
            game.processPass(myColor);
        }
    }

    /**
     * System oceny ruchu. Im więcej punktów, tym lepszy ruch.
     */
    private double evaluateMove(Board board, int x, int y) {
        double score = 0;
        int size = board.getSize();

        board.setStone(x, y, myColor);
        int liberties = countLiberties(board, x, y, myColor, new HashSet<>());
        board.setStone(x, y, StoneColor.EMPTY);

        if (liberties == 0) return -1000;
        if (liberties == 1) score -= 50;

        int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
        boolean touchesEnemy = false;

        for (int[] n : neighbors) {
            int nx = n[0];
            int ny = n[1];
            if (nx >= 0 && nx < size && ny >= 0 && ny < size) {
                StoneColor neighborColor = board.getStone(nx, ny);

                if (neighborColor == opponentColor) {
                    touchesEnemy = true;
                    int enemyLiberties = countLiberties(board, nx, ny, opponentColor, new HashSet<>());
                    if (enemyLiberties == 1) {
                        score += 1000;
                    } else if (enemyLiberties == 2) {
                        score += 20;
                    }
                } else if (neighborColor == myColor) {
                    int myGroupLiberties = countLiberties(board, nx, ny, myColor, new HashSet<>());
                    if (myGroupLiberties == 1) {
                        score += 800;
                    } else {
                        score += 5;
                    }
                }
            }
        }

        if (!touchesEnemy) {
            double distFromCenter = Math.abs(x - size / 2.0) + Math.abs(y - size / 2.0);
            score -= distFromCenter;

            if (x == 0 || x == size - 1 || y == 0 || y == size - 1) score -= 10;
        }

        return score;
    }

    private int countLiberties(Board board, int x, int y, StoneColor color, Set<String> visited) {
        if (x < 0 || x >= board.getSize() || y < 0 || y >= board.getSize()) return 0;
        String key = x + "," + y;
        if (visited.contains(key)) return 0;
        visited.add(key);

        StoneColor stone = board.getStone(x, y);
        if (stone == StoneColor.EMPTY) return 1;
        if (stone != color) return 0;

        int libs = 0;
        libs += countLiberties(board, x + 1, y, color, visited);
        libs += countLiberties(board, x - 1, y, color, visited);
        libs += countLiberties(board, x, y + 1, color, visited);
        libs += countLiberties(board, x, y - 1, color, visited);
        return libs;
    }
}