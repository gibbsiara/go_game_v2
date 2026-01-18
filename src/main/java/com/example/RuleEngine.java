package com.example;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Handles the specific rules and logic of the game of Go.
 * Responsible for move validation, capturing stones, and calculating the final score.
 */
public class RuleEngine {
    private ClientHandler playerBlack;
    private ClientHandler playerWhite;
    private StoneColor[][] previousBoardState = null;

    /**
     * Checks if a proposed move is legal according to Go rules.
     * Validates bounds, occupancy, suicide rules, and the Ko rule.
     * @param board The current board state.
     * @param x The X-coordinate of the move.
     * @param y The Y-coordinate of the move.
     * @param playerColor The color of the stone being placed.
     * @return true if the move is valid, false otherwise.
     */
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

    /**
     * Executes a valid move on the board and removes captured stones.
     * Updates the previous board state for future Ko checks.
     * @param board The current board being played on.
     * @param x The X-coordinate.
     * @param y The Y-coordinate.
     * @param playerColor The color of the stone.
     * @return The number of stones captured by this move.
     */
    public int playMove(Board board, int x, int y, StoneColor playerColor) {
        previousBoardState = board.getGridCopy();
        board.setStone(x, y, playerColor);
        
        List<int[]> removed = removeDeadOpponentGroups(board, x, y, playerColor);
        return removed.size();
    }

    /**
     * Calculates the final score based on territory and prisoners.
     * Analyzes empty regions to determine ownership (Black or White territory).
     * @param board The final board state.
     * @param blackPrisoners Total prisoners held by Black.
     * @param whitePrisoners Total prisoners held by White.
     * @return A string summary of the scores.
     */
    public String calculateScore(Board board, int blackPrisoners, int whitePrisoners) {
        int size = board.getSize();
        boolean[][] visited = new boolean[size][size];
        List<Region> allRegions = new ArrayList<>();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board.getStone(x, y) == StoneColor.EMPTY && !visited[x][y]) {
                    allRegions.add(analyzeRegion(board, visited, x, y));
                }
            }
        }

        Set<String> neutralPoints = new HashSet<>();
        for (Region r : allRegions) {
            if (r.borderColors.size() > 1) {
                for (int[] p : r.points) {
                    neutralPoints.add(p[0] + "," + p[1]);
                }
            }
        }

        Set<String> stonesInSeki = findStonesInSeki(board, neutralPoints);

        int blackTerritory = 0;
        int whiteTerritory = 0;

        for (Region r : allRegions) {
            if (r.borderColors.size() == 1) {
                StoneColor owner = r.borderColors.iterator().next();
                
                boolean touchesSeki = false;
                for (int[] bp : r.borderStones) {
                    if (stonesInSeki.contains(bp[0] + "," + bp[1])) {
                        touchesSeki = true;
                        break;
                    }
                }

                if (!touchesSeki) {
                    if (owner == StoneColor.BLACK) {
                        blackTerritory += r.points.size();
                    } else if (owner == StoneColor.WHITE) {
                        whiteTerritory += r.points.size();
                    }
                }
            }
        }

        int blackTotal = blackTerritory + blackPrisoners;
        int whiteTotal = whiteTerritory + whitePrisoners;

        return "Czarne: " + blackTotal + " (Teren: " + blackTerritory + ", Jeńcy: " + blackPrisoners + ")\n" +
               "Białe: " + whiteTotal + " (Teren: " + whiteTerritory + ", Jeńcy: " + whitePrisoners + ")";
    }

    /**
     * Helper class representing a connected region of empty points.
     * Used for territory analysis.
     */
    private static class Region {
        List<int[]> points = new ArrayList<>();
        Set<StoneColor> borderColors = new HashSet<>();
        List<int[]> borderStones = new ArrayList<>();
    }

    /**
     * Performs a Breadth-First Search (BFS) to identify a connected region of empty intersections.
     * Determines which stone colors border this region to assign territory.
     * @param board The game board.
     * @param visited Array tracking visited nodes to prevent loops.
     * @param startX Starting X coordinate.
     * @param startY Starting Y coordinate.
     * @return A Region object containing the empty points and bordering stone info.
     */
    private Region analyzeRegion(Board board, boolean[][] visited, int startX, int startY) {
        Region region = new Region();
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            region.points.add(p);

            int[][] neighbors = {{p[0]+1, p[1]}, {p[0]-1, p[1]}, {p[0], p[1]+1}, {p[0], p[1]-1}};
            for (int[] n : neighbors) {
                if (isOnBoard(board, n[0], n[1])) {
                    StoneColor color = board.getStone(n[0], n[1]);
                    if (color == StoneColor.EMPTY) {
                        if (!visited[n[0]][n[1]]) {
                            visited[n[0]][n[1]] = true;
                            queue.add(new int[]{n[0], n[1]});
                        }
                    } else {
                        region.borderColors.add(color);
                        region.borderStones.add(new int[]{n[0], n[1]});
                    }
                }
            }
        }
        return region;
    }

    /**
     * Identifies stones that are in a "Seki" (mutual life) state.
     * Checks if groups touch neutral points that neither player can claim.
     * @param board The game board.
     * @param neutralPoints A set of coordinates representing neutral territory.
     * @return A set of string keys ("x,y") representing stones in Seki.
     */
    private Set<String> findStonesInSeki(Board board, Set<String> neutralPoints) {
        Set<String> inSeki = new HashSet<>();
        int size = board.getSize();
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                StoneColor color = board.getStone(x, y);
                if (color != StoneColor.EMPTY) {
                    if (touchesNeutral(x, y, neutralPoints)) {
                        markGroupAsSeki(board, x, y, color, inSeki);
                    }
                }
            }
        }
        return inSeki;
    }

    /**
     * Checks if a specific board coordinate is adjacent to a neutral point.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param neutralPoints The set of pre-calculated neutral points.
     * @return true if adjacent to a neutral point.
     */
    private boolean touchesNeutral(int x, int y, Set<String> neutralPoints) {
        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};
        for (int[] n : neighbors) {
            if (neutralPoints.contains(n[0] + "," + n[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively marks an entire group of connected stones as being in Seki.
     * @param board The game board.
     * @param x Current X coordinate.
     * @param y Current Y coordinate.
     * @param color The color of the group being marked.
     * @param sekiSet The set to populate with Seki coordinates.
     */
    private void markGroupAsSeki(Board board, int x, int y, StoneColor color, Set<String> sekiSet) {
        String key = x + "," + y;
        if (sekiSet.contains(key)) return;
        sekiSet.add(key);

        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};
        for (int[] n : neighbors) {
            if (isOnBoard(board, n[0], n[1]) && board.getStone(n[0], n[1]) == color) {
                markGroupAsSeki(board, n[0], n[1], color, sekiSet);
            }
        }
    }

    /**
     * Simulates a move to check if it would capture any opponent stones.
     * This is used to allow "suicide" moves if they result in a capture.
     * @param board The board state.
     * @param x X coordinate of the proposed move.
     * @param y Y coordinate of the proposed move.
     * @param playerColor The player making the move.
     * @return true if the move results in capturing opponent stones.
     */
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

    /**
     * Removes groups of opponent stones that have zero liberties.
     * @param board The board state.
     * @param x The X coordinate of the move that just happened.
     * @param y The Y coordinate of the move that just happened.
     * @param playerColor The color of the player who just moved.
     * @return A list of coordinates of the removed stones.
     */
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

    /**
     * Recursively collects all connected stones of a specific color into a list.
     * @param board The board state.
     * @param x Current X coordinate.
     * @param y Current Y coordinate.
     * @param color The color of the group being collected.
     * @param group The list to populate with group coordinates.
     */
    private void collectGroup(Board board, int x, int y, StoneColor color, List<int[]> group) {
        if (board.getStone(x, y) != color) return;
        for(int[] pos : group) if (pos[0] == x && pos[1] == y) return;
        group.add(new int[]{x, y});
        int[][] neighbors = {{x+1, y}, {x-1, y}, {x, y+1}, {x, y-1}};
        for (int[] n : neighbors) if (isOnBoard(board, n[0], n[1])) collectGroup(board, n[0], n[1], color, group);
    }

    /**
     * Counts the number of liberties (empty adjacent intersections) for a group of stones.
     * @param board The board state.
     * @param x X coordinate of a stone in the group.
     * @param y Y coordinate of a stone in the group.
     * @return The number of unique liberties available to the group.
     */
    private int countLiberties(Board board, int x, int y) {
        Set<String> visited = new HashSet<>();
        Set<String> liberties = new HashSet<>();
        findGroupAndLiberties(board, x, y, board.getStone(x, y), visited, liberties);
        return liberties.size();
    }

    /**
     * Recursive helper to traverse a group and identify unique liberties.
     * @param board The board state.
     * @param x Current X coordinate.
     * @param y Current Y coordinate.
     * @param color The color of the group.
     * @param visited Set of visited stones to prevent infinite recursion.
     * @param liberties Set to populate with liberty coordinates.
     */
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
    
    /**
     * Sets the active player handlers for communication.
     * @param black The handler for the Black player.
     * @param white The handler for the White player.
     */
    public void setPlayers(ClientHandler black, ClientHandler white) {
        this.playerBlack = black;
        this.playerWhite = white;
    }

    private void notifyPlayer(StoneColor color, String msg) {
        if (color == StoneColor.BLACK && playerBlack != null) playerBlack.sendMessage(msg);
        else if (color == StoneColor.WHITE && playerWhite != null) playerWhite.sendMessage(msg);
    }
}