package com.example;

public class MoveCommand implements Command {
    private Game game;
    private int x;
    private int y;
    private StoneColor color;

    public MoveCommand(Game game, int x, int y, StoneColor color) {
        this.game = game;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    @Override
    public void execute() {
        game.processMove(x, y, color);
    }
}