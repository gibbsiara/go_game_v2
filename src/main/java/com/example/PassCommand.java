package com.example;

/**
 * Implementation of the Command pattern representing a pass action.
 * Triggers the pass logic in the game engine.
 */
public class PassCommand implements Command {
    private Game game;
    private StoneColor playerColor;

    /**
     * Initializes a new PassCommand.
     * @param game The Game instance where the pass will be processed.
     * @param playerColor The color of the player who is passing.
     */
    public PassCommand(Game game, StoneColor playerColor) {
        this.game = game;
        this.playerColor = playerColor;
    }

    /**
     * Executes the pass command by calling the game's processPass method.
     */
    @Override
    public void execute() {
        game.processPass(playerColor);
    }
}