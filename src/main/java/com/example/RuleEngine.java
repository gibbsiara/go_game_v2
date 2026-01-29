package com.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles the specific rules and logic of the game of Go.
 * Responsible for move validation, capturing stones, and calculating the final score.
 */
public class RuleEngine {
    private Player playerBlack;
    private Player playerWhite;

    private StoneColor[][] stateOneTurnAgo = null;
    private StoneColor[][] stateTwoTurnsAgo = null;

    private int lastCapturedCount = 0;

    /**
     * Checks if a proposed move is legal according to Go rules.
     * Validates bounds, occupancy, suicide rules, and the Ko rule.
     */
    public boolean isMoveValid(Board board, int x, int y, StoneColor playerColor) {
        lastCapturedCount = 0;

        if (x < 0 || x >= board.getSize() || y < 0 || y >= board.getSize()) {
            notifyPlayer(playerColor, "MESSAGE Błąd: Ruch poza planszą!");
            return false;
        }

        if (board.getStone(x, y) != StoneColor.EMPTY) {
            notifyPlayer(playerColor, "MESSAGE Błąd: Pole jest już zajęte!");
            return false;
        }

        StoneColor originalColor = board.getStone(x, y);
        board.setStone(x, y, playerColor);

        boolean captures = checkCaptures(board, x, y, playerColor);
        boolean hasLiberties = hasLiberties(board, x, y, playerColor);

        if (!hasLiberties && !captures) {
            board.setStone(x, y, originalColor);
            notifyPlayer(playerColor, "MESSAGE Błąd: Ruch samobójczy jest zabroniony!");
            return false;
        }

        StoneColor[][] stateBeforeCapture = board.getGridCopy();

        if (captures) {
            removeDeadStones(board, x, y, playerColor);
        }

        if (stateTwoTurnsAgo != null && board.hasSameStateAs(stateTwoTurnsAgo)) {
            restoreBoardState(board, stateBeforeCapture);
            board.setStone(x, y, StoneColor.EMPTY);
            lastCapturedCount = 0;

            notifyPlayer(playerColor, "MESSAGE Błąd: Zasada KO (nie możesz powtórzyć pozycji)!");
            return false;
        }

        stateTwoTurnsAgo = stateOneTurnAgo;
        stateOneTurnAgo = board.getGridCopy();

        return true;
    }

    /**
     * Zwraca liczbę kamieni zbitych w ostatnim zaakceptowanym ruchu.
     * Wywoływane przez Game.java po isMoveValid() == true.
     */
    public int getLastCapturedCount() {
        return lastCapturedCount;
    }


    private boolean checkCaptures(Board board, int x, int y, StoneColor playerColor) {
        StoneColor opponentColor = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
        int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};

        for (int[] n : neighbors) {
            int nx = n[0];
            int ny = n[1];
            if (isOnBoard(board, nx, ny) && board.getStone(nx, ny) == opponentColor) {
                if (!hasLiberties(board, nx, ny, opponentColor)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeDeadStones(Board board, int x, int y, StoneColor playerColor) {
        StoneColor opponentColor = (playerColor == StoneColor.BLACK) ? StoneColor.WHITE : StoneColor.BLACK;
        int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};

        for (int[] n : neighbors) {
            int nx = n[0];
            int ny = n[1];
            if (isOnBoard(board, nx, ny) && board.getStone(nx, ny) == opponentColor) {
                if (!hasLiberties(board, nx, ny, opponentColor)) {
                    Set<String> group = new HashSet<>();
                    findGroupAndLiberties(board, nx, ny, opponentColor, group, new HashSet<>());

                    lastCapturedCount += group.size();

                    for (String pos : group) {
                        String[] coords = pos.split(",");
                        board.setStone(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), StoneColor.EMPTY);
                    }
                }
            }
        }
    }


    /**
     * Oblicza terytorium dla obu graczy.
     * Zwraca tablicę: [punktyCzarnego, punktyBiałego].
     * Terytorium to puste pola otoczone wyłącznie przez kamienie jednego koloru.
     */
    public int[] countTerritory(Board board) {
        int blackTerritory = 0;
        int whiteTerritory = 0;
        int size = board.getSize();
        boolean[][] visited = new boolean[size][size];

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board.getStone(x, y) == StoneColor.EMPTY && !visited[x][y]) {
                    RegionResult result = exploreRegion(board, x, y, visited);

                    if (result.surroundedByBlack && !result.surroundedByWhite) {
                        blackTerritory += result.size;
                    } else if (!result.surroundedByBlack && result.surroundedByWhite) {
                        whiteTerritory += result.size;
                    }
                }
            }
        }
        return new int[]{blackTerritory, whiteTerritory};
    }

    private static class RegionResult {
        int size = 0;
        boolean surroundedByBlack = false;
        boolean surroundedByWhite = false;
    }

    private RegionResult exploreRegion(Board board, int startX, int startY, boolean[][] visited) {
        RegionResult result = new RegionResult();
        List<int[]> queue = new ArrayList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;

        int head = 0;
        while(head < queue.size()){
            int[] curr = queue.get(head++);
            int cx = curr[0];
            int cy = curr[1];
            result.size++;

            int[][] neighbors = {{cx+1, cy}, {cx-1, cy}, {cx, cy+1}, {cx, cy-1}};
            for (int[] n : neighbors) {
                int nx = n[0];
                int ny = n[1];

                if (isOnBoard(board, nx, ny)) {
                    StoneColor neighbor = board.getStone(nx, ny);
                    if (neighbor == StoneColor.EMPTY) {
                        if (!visited[nx][ny]) {
                            visited[nx][ny] = true;
                            queue.add(new int[]{nx, ny});
                        }
                    } else if (neighbor == StoneColor.BLACK) {
                        result.surroundedByBlack = true;
                    } else if (neighbor == StoneColor.WHITE) {
                        result.surroundedByWhite = true;
                    }
                }
            }
        }
        return result;
    }

    private void restoreBoardState(Board board, StoneColor[][] snapshot) {
        for(int i=0; i<board.getSize(); i++) {
            for(int j=0; j<board.getSize(); j++) {
                board.setStone(i, j, snapshot[i][j]);
            }
        }
    }

    private boolean hasLiberties(Board board, int x, int y, StoneColor color) {
        Set<String> visited = new HashSet<>();
        Set<String> liberties = new HashSet<>();
        findGroupAndLiberties(board, x, y, color, visited, liberties);
        return !liberties.isEmpty();
    }

    private void findGroupAndLiberties(Board board, int x, int y, StoneColor color, Set<String> visited, Set<String> liberties) {
        String pos = x + "," + y;
        if (visited.contains(pos)) return;
        visited.add(pos);

        int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
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

    private boolean isOnBoard(Board board, int x, int y) {
        return x >= 0 && x < board.getSize() && y >= 0 && y < board.getSize();
    }

    public void setPlayers(Player black, Player white) {
        this.playerBlack = black;
        this.playerWhite = white;
    }

    private void notifyPlayer(StoneColor color, String msg) {
        if (color == StoneColor.BLACK && playerBlack != null) {
            playerBlack.sendMessage(msg);
        } else if (color == StoneColor.WHITE && playerWhite != null) {
            playerWhite.sendMessage(msg);
        }
    }
}