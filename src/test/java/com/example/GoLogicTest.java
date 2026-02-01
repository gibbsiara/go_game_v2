package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GoLogicTest {

    private RuleEngine ruleEngine;
    private Board board;
    private final int BOARD_SIZE = 9;

    @BeforeEach
    public void setUp() {
        ruleEngine = new RuleEngine();
        board = new Board(BOARD_SIZE);
        ruleEngine.setPlayers(new MockPlayer(), new MockPlayer());
    }

    @Test
    public void testPlaceStone() {
        boolean isValid = ruleEngine.isMoveValid(board, 0, 0, StoneColor.BLACK);

        assertTrue(isValid, "Ruch na puste pole powinien być dozwolony");
        assertEquals(StoneColor.BLACK, board.getStone(0, 0), "Na polu 0,0 powinien być czarny kamień");
    }

    @Test
    public void testMoveOutOfBounds() {
        boolean isValid = ruleEngine.isMoveValid(board, -1, 0, StoneColor.BLACK);
        assertFalse(isValid, "Ruch poza planszę (ujemny) nie powinien być dozwolony");

        isValid = ruleEngine.isMoveValid(board, BOARD_SIZE, 0, StoneColor.BLACK);
        assertFalse(isValid, "Ruch poza planszę (zbyt duży indeks) nie powinien być dozwolony");
    }

    @Test
    public void testOccupiedSpot() {
        board.setStone(2, 2, StoneColor.BLACK);
        boolean isValid = ruleEngine.isMoveValid(board, 2, 2, StoneColor.WHITE);
        assertFalse(isValid, "Nie można stawiać kamienia na zajętym polu");
    }

    @Test
    public void testCaptureSingleStone() {
        board.setStone(1, 0, StoneColor.BLACK);
        board.setStone(0, 1, StoneColor.BLACK);
        board.setStone(1, 2, StoneColor.BLACK);
        board.setStone(1, 1, StoneColor.WHITE);

        boolean isValid = ruleEngine.isMoveValid(board, 2, 1, StoneColor.BLACK);

        assertTrue(isValid, "Ruch zbijający powinien być dozwolony");
        assertEquals(StoneColor.EMPTY, board.getStone(1, 1), "Zbity kamień powinien zniknąć z planszy");
        assertEquals(StoneColor.BLACK, board.getStone(2, 1), "Kamień stawiający powinien zostać na planszy");
    }

    @Test
    public void testSuicideMove() {
        board.setStone(1, 0, StoneColor.BLACK);
        board.setStone(0, 1, StoneColor.BLACK);
        board.setStone(2, 1, StoneColor.BLACK);
        board.setStone(1, 2, StoneColor.BLACK);

        boolean isValid = ruleEngine.isMoveValid(board, 1, 1, StoneColor.WHITE);

        assertFalse(isValid, "Ruch samobójczy powinien być zabroniony");
        assertEquals(StoneColor.EMPTY, board.getStone(1, 1), "Pole po nieudanym samobójstwie powinno zostać puste");
    }

    private static class MockPlayer implements Player {
        @Override public void sendMessage(String msg) {}
    }
}