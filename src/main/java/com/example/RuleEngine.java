package com.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuleEngine {
    private ClientHandler playerBlack;
    private ClientHandler playerWhite;
    private StoneColor[][] previousBoardState = null; 

    public boolean isMoveValid(Board board, int x, int y, StoneColor playerColor) {
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

        boolean capturesOpponent = checkCapturesSimulation(board, x, y, playerColor);
        int selfLiberties = countLiberties(board, x, y);

        if (selfLiberties == 0 && !capturesOpponent) {
            board.setStone(x, y, originalColor);
            notifyPlayer(playerColor, "MESSAGE: Ruch samobójczy jest zabroniony!");
            return false;
        }

        if (previousBoardState != null) {
            List<int[]> capturedStones = removeDeadOpponentGroups(board, x, y, playerColor);
            boolean isKo = board.hasSameStateAs(previousBoardState);

            for (int[] pos : capturedStones) {
                board.setStone(pos[0], pos[1], (playerColor == StoneColor.BLACK ? StoneColor.WHITE : StoneColor.BLACK));
            }
            board.setStone(x, y, originalColor);

            if (isKo) {
                notifyPlayer(playerColor, "MESSAGE: Zasada KO! Nie możesz powtórzyć pozycji planszy.");
                return false;
            }
        } else {
            board.setStone(x, y, originalColor);
        }

        return true;
    }

    public int playMove(Board board, int x, int y, StoneColor playerColor) {
        previousBoardState = board.getGridCopy();
        board.setStone(x, y, playerColor);
        
        List<int[]> removed = removeDeadOpponentGroups(board, x, y, playerColor);
        return removed.size();
    }

    public String calculateScore(Board board, int blackPrisoners, int whitePrisoners) {
        int blackTerritory = 0;
        int whiteTerritory = 0;
        int size = board.getSize();
        boolean[][] visited = new boolean[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board.getStone(x, y) == StoneColor.EMPTY && !visited[x][y]) {
                    TerritoryResult result = analyzeTerritory(board, visited, x, y);
                    if (result.owner == StoneColor.BLACK) {
                        blackTerritory += result.count;
                    } else if (result.owner == StoneColor.WHITE) {
                        whiteTerritory += result.count;
                    }
                }
            }
        }

        int blackTotal = blackTerritory + blackPrisoners;
        int whiteTotal = whiteTerritory + whitePrisoners;

        return "Czarne: " + blackTotal + " (Teren: " + blackTerritory + ", Jeńcy: " + blackPrisoners + ")\n" +
               "Białe: " + whiteTotal + " (Teren: " + whiteTerritory + ", Jeńcy: " + whitePrisoners + ")";
    }

    private TerritoryResult analyzeTerritory(Board board, boolean[][] visited, int startX, int startY) {
        List<int[]> points = new ArrayList<>();
        Set<StoneColor> borderColors = new HashSet<>();
        
        List<int[]> queue = new ArrayList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int[] p = queue.remove(0);
            points.add(p);
            int cx = p[0], cy = p[1];

            int[][] neighbors = {{cx+1, cy}, {cx-1, cy}, {cx, cy+1}, {cx, cy-1}};
            for (int[] n : neighbors) {
                int nx = n[0], ny = n[1];
                if (isOnBoard(board, nx, ny)) {
                    StoneColor color = board.getStone(nx, ny);
                    if (color == StoneColor.EMPTY) {
                        if (!visited[nx][ny]) {
                            visited[nx][ny] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    } else {
                        borderColors.add(color);
                    }
                }
            }
        }

        StoneColor owner = StoneColor.EMPTY;
        if (borderColors.size() == 1) {
            if (borderColors.contains(StoneColor.BLACK)) owner = StoneColor.BLACK;
            if (borderColors.contains(StoneColor.WHITE)) owner = StoneColor.WHITE;
        }

        return new TerritoryResult(owner, points.size());
    }

    private static class TerritoryResult {
        StoneColor owner;
        int count;
        public TerritoryResult(StoneColor owner, int count) {
            this.owner = owner;
            this.count = count;
        }
    }

    private boolean checkCapturesSimulation(Board board, int x, int y, StoneColor playerColor) {
        StoneColor opponentColor = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};
        for (int[] n : neighbors) {
            int nx = n[0], ny = n[1];
            if (isOnBoard(board, nx, ny) && board.getStone(nx, ny) == opponentColor) {
                if (countLiberties(board, nx, ny) == 0) return true;
            }
        }
        return false;
    }

    private List<int[]> removeDeadOpponentGroups(Board board, int x, int y, StoneColor playerColor) {
        List<int[]> removedStones = new ArrayList<>();
        StoneColor opponentColor = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};
        
        for (int[] n : neighbors) {
            int nx = n[0], ny = n[1];
            if (isOnBoard(board, nx, ny) && board.getStone(nx, ny) == opponentColor) {
                if (countLiberties(board, nx, ny) == 0) {
                    collectGroup(board, nx, ny, opponentColor, removedStones);
                }
            }
        }
        for (int[] pos : removedStones) board.setStone(pos[0], pos[1], StoneColor.EMPTY);
        return removedStones;
    }

    private void collectGroup(Board board, int x, int y, StoneColor color, List<int[]> group) {
        if (board.getStone(x, y) != color) return;
        for(int[] pos : group) if (pos[0] == x && pos[1] == y) return;
        group.add(new int[]{x, y});
        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};
        for (int[] n : neighbors) if (isOnBoard(board, n[0], n[1])) collectGroup(board, n[0], n[1], color, group);
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
                if (neighborStone == StoneColor.EMPTY) liberties.add(nx + "," + ny);
                else if (neighborStone == color) findGroupAndLiberties(board, nx, ny, color, visited, liberties);
            }
        }
    }

    private boolean isOnBoard(Board board, int x, int y) {
        return x >= 0 && x < board.getSize() && y >= 0 && y < board.getSize();
    }
    
    public void setPlayers(ClientHandler black, ClientHandler white) {
        this.playerBlack = black;
        this.playerWhite = white;
    }

    private void notifyPlayer(StoneColor color, String msg) {
        if (color == StoneColor.BLACK && playerBlack != null) playerBlack.sendMessage(msg);
        else if (color == StoneColor.WHITE && playerWhite != null) playerWhite.sendMessage(msg);
    }
}