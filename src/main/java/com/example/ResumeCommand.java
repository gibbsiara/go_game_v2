package com.example;

/**
 * Implementation of the Command pattern representing a resume action.
 * Used to continue the game after a dispute or pause (e.g., dead stone disagreement).
 */
public class ResumeCommand implements Command {
    private Game game;
    private StoneColor playerColor;

    /**
     * Initializes a new ResumeCommand.
     * @param game The Game instance where the resume action will be processed.
     * @param playerColor The color of the player requesting the resume.
     */
    public ResumeCommand(Game game, StoneColor playerColor) {
        this.game = game;
        this.playerColor = playerColor;
    }

    /**
     * Executes the resume command by calling the game's processResume method.
     */
    @Override
    public void execute() {
        game.processResume(playerColor);
    }
}