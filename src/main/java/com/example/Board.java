package com.example;
/**
 * Represents the logical structure of a Go board.
 * It maintains the state of every intersection (grid point) on the board.
 */
public class Board {

    private int size;
    private StoneColor[][] grid;
    /**
     * Initializes a new board with a specific size and fills it with EMPTY states.
     *  @param size The number of lines per side (e.g., 9, 13, or 19).
     */
    public Board(int size) {
        this.size = size;
        this.grid = new StoneColor[size][size];
        initializeBoard();
    }
    
    public void initializeBoard() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = StoneColor.EMPTY;
            }
        }
    }
    /**
     * Retrieves the stone color at the specified coordinates.
     * @param x The X-coordinate (column).
     * @param y The Y-coordinate (row).
     * @return The StoneColor at the position, or null if coordinates are out of bounds.
     */


    public StoneColor getStone(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            return null;
        }
        return grid[x][y];
    }
    /**
     * Places a stone of a specific color at the given coordinates.
     * * @param x The X-coordinate.
     * @param y The Y-coordinate.
     * @param color The StoneColor to set at the position.
     */

    public void setStone(int x, int y, StoneColor color) {
        grid[x][y] = color;
    }

    public int getSize() {
        return size;
    }
    /**
     * Returns a deep copy of the current board grid.
     * @return A 2D array representing the current state of the board.
     */
    public StoneColor[][] getGridCopy() {
        StoneColor[][] copy = new StoneColor[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, size);
        }
        return copy;
    }
    /**
     * Compares the current board state with another grid.
     * @param otherGrid The grid to compare against.
     * @return true if the states are identical; false otherwise.
     */
    public boolean hasSameStateAs(StoneColor[][] otherGrid) {
        if (otherGrid == null) return false;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] != otherGrid[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid[x][y] == StoneColor.EMPTY) {
                    str.append(' ');
                } else if (grid[x][y] == StoneColor.BLACK) {
                    str.append('X');
                } else {
                    str.append('O');
                }

                if (x < size - 1) {
                    str.append("-");
                }
            }
            str.append('\n');
        }
        return str.toString();
    }
}