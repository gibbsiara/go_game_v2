package com.example;

/**
 * Implementation of the Command pattern representing a surrender action.
 * Allows a player to concede the game, declaring the opponent the winner.
 */
public class SurrenderCommand implements Command {
    private Game game;
    private StoneColor playerColor;

    /**
     * Initializes a new SurrenderCommand.
     * @param game The Game instance where the surrender will be processed.
     * @param playerColor The color of the player who is surrendering.
     */
    public SurrenderCommand(Game game, StoneColor playerColor) {
        this.game = game;
        this.playerColor = playerColor;
    }

    /**
     * Executes the surrender command by calling the game's processSurrender method.
     */
    @Override
    public void execute() {
        game.processSurrender(playerColor);
    }
}