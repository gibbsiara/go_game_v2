package com.example;
/**
 * Implementation of the Command pattern representing a move action.
 * Encapsulates the target game instance, coordinates, and player color.
 */
public class MoveCommand implements Command {
    private Game game;
    private int x;
    private int y;
    private StoneColor color;
    /**
     * Initializes a new MoveCommand.
     * @param game The Game instance where the move will be processed.
     * @param x The target X-coordinate.
     * @param y The target Y-coordinate.
     * @param color The color of the stone being placed.
     */
    public MoveCommand(Game game, int x, int y, StoneColor color) {
        this.game = game;
        this.x = x;
        this.y = y;
        this.color = color;
    }
    /**
     * Executes the move by calling the game's processMove method.
     */
    @Override
    public void execute() {
        game.processMove(x, y, color);
    }
}