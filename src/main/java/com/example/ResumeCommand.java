package com.example;

public class ResumeCommand implements Command {
    private Game game;
    private StoneColor playerColor;

    public ResumeCommand(Game game, StoneColor playerColor) {
        this.game = game;
        this.playerColor = playerColor;
    }

    @Override
    public void execute() {
        game.processResume(playerColor);
    }
}