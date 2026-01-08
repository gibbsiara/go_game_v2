package com.example;

public class PassCommand implements Command {
    private Game game;
    private StoneColor playerColor;

    public PassCommand(Game game, StoneColor playerColor) {
        this.game = game;
        this.playerColor = playerColor;
    }

    @Override
    public void execute() {
        game.processPass(playerColor);
    }
}