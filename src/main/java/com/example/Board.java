package com.example;

public class Board {
    private int size;
    private StoneColor[][] grid;

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

    public StoneColor getStone(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            return null;
        }
        return grid[x][y];
    }

    public void setStone(int x, int y, StoneColor color) {
        grid[x][y] = color;
    }

    public int getSize() {
        return size;
    }

    public StoneColor[][] getGridCopy() {
        StoneColor[][] copy = new StoneColor[size][size];
        for (int i = 0; i < size; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, size);
        }
        return copy;
    }

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