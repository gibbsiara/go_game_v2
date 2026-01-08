package com.example;

public class SurrenderCommand implements Command {
    private Game game;
    private StoneColor playerColor;

    public SurrenderCommand(Game game, StoneColor playerColor) {
        this.game = game;
        this.playerColor = playerColor;
    }

    @Override
    public void execute() {
        game.processSurrender(playerColor);
    }
}