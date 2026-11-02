package com.github.lfbr0;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.engine.Engine;
import com.github.lfbr0.engine.EngineMove;
import com.github.lfbr0.move.Move;
import com.github.lfbr0.move.MoveHandler;
import com.github.lfbr0.move.MoveValidator;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */
public class EngineTests {

    @Test
    public void capturesFreeQueenAsWhite() {
        GameState state = new GameState(
                "7k/8/8/8/8/8/q7/R6K w - - 0 1");

        assertBestMoveAmong(state, 1, "a1a2");
    }

    @Test
    public void capturesFreeQueenAsBlack() {
        GameState state = new GameState(
                "r6k/Q7/8/8/8/8/8/7K b - - 0 1");

        assertBestMoveAmong(state, 1, "a8a7");
    }

    @Test
    public void avoidsQueenCaptureThatLosesToRookRecapture() {
        GameState state = new GameState(
                "r6k/8/8/8/8/8/q7/Q6K w - - 0 1");

        EngineMove best = bestMove(state, 2);

        assertNotEquals("a1a2", best.move().getUciMoveString());
    }

    @Test
    public void findsMateInOne() {
        GameState state = new GameState(
                "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1");

        assertBestMoveAmong(state, 1, "g6g7", "g6h5", "g6g8");
        assertEngineForcesMate(state, true, 1);
    }

    @Test
    public void findsMateInTwo() {
        // white: king f5, queen g4; black: king h8
        GameState state = new GameState(
                "7k/8/8/5K2/6Q1/8/8/8 w - - 0 1");

        // either king move allows mate on white's next turn
        assertBestMoveAmong(state, 3, "f5f6", "f5g6");

        assertFalse("This position must not already have mate in one",
                canForceMate(state, true, 1));
        assertEngineForcesMate(state, true, 3);
    }

    @Test
    public void findsMateInThree() {
        // white: king f4, queen g3; black: king h8
        GameState state = new GameState(
                "7k/8/8/8/5K2/6Q1/8/8 w - - 0 1");

        assertBestMoveAmong(state, 5, "f4e5", "f4f5");

        assertFalse("This position must not have a forced mate in two",
                canForceMate(state, true, 3));
        assertEngineForcesMate(state, true, 5);
    }

    @Test
    public void findsKnightForkOfKingAndQueen() {
        // e7 checks the king on g8 and attacks the queen on c8
        GameState state = new GameState(
                "2q3k1/8/8/3N4/8/8/8/K7 w - - 0 1");

        assertBestMoveAmong(state, 3, "d5e7");

        GameState forked = MoveHandler.applyMove(state, new Move("d5e7"));
        assertTrue(MoveValidator.isInCheck(forked));

        // after every king escape, white should take the queen
        Set<Move> replies = MoveValidator.getLegalMoves(forked);
        assertFalse(replies.isEmpty());

        for (Move reply : replies) {
            GameState afterReply = MoveHandler.applyMove(forked, reply);
            assertBestMoveAmong(afterReply, 1, "e7c8");
        }
    }

    @Test
    public void pinnedRookCapturesItsAttacker() {
        // rook e2 shields king e1 from the black rook on e8
        GameState state = new GameState(
                "4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1");

        assertBestMoveAmong(state, 1, "e2e8");
    }

    @Test
    public void pinnedRookCannotLeaveKingsFile() {
        GameState state = new GameState(
                "4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1");

        EngineMove[] moves = Engine.bestMoves(state, 100, 1);
        int rookOrigin = Move.squareStringToIndex("e2");

        boolean foundRookMove = false;

        for (EngineMove candidate : moves) {
            Move move = candidate.move();

            if (move.getFrom() == rookOrigin) {
                foundRookMove = true;

                // moving sideways would expose our king
                assertEquals(4, move.getTo() % 8);
            }
        }

        assertTrue("Expected legal moves for the pinned rook", foundRookMove);
    }

    @Test
    public void pinnedPawnCapturesItsAttacker() {
        // moving forward or capturing c5 exposes the king on a1
        GameState state = new GameState(
                "7k/8/8/2n1b3/3P4/8/8/K7 w - - 0 1");

        EngineMove[] moves = Engine.bestMoves(state, 100, 1);
        int pawnOrigin = Move.squareStringToIndex("d4");

        Set<String> pawnMoves = Arrays.stream(moves)
                .map(EngineMove::move)
                .filter(move -> move.getFrom() == pawnOrigin)
                .map(Move::getUciMoveString)
                .collect(Collectors.toSet());

        assertEquals(
                new HashSet<>(Arrays.asList("d4e5")),
                pawnMoves);
    }

    @Test
    public void returnsOnlyLegalAndDistinctMoves() {
        GameState state = startingPosition();

        EngineMove[] moves = Engine.bestMoves(state, 100, 1);
        Set<Move> returnedMoves = new HashSet<>();

        for (EngineMove candidate : moves) {
            assertTrue(MoveValidator.isLegalMove(state, candidate.move()));
            assertTrue("Duplicate move", returnedMoves.add(candidate.move()));
        }

        assertEquals(MoveValidator.getLegalMoves(state), returnedMoves);
    }

    @Test
    public void limitsNumberOfReturnedMoves() {
        assertEquals(3,
                Engine.bestMoves(startingPosition(), 3, 1).length);
    }

    @Test
    public void returnsAllMovesWhenCountExceedsAvailableMoves() {
        assertEquals(20,
                Engine.bestMoves(startingPosition(), 100, 1).length);
    }

    @Test
    public void zeroCountReturnsNoMoves() {
        assertEquals(0,
                Engine.bestMoves(startingPosition(), 0, 1).length);
    }

    @Test
    public void checkmatedSideHasNoMoves() {
        GameState state = new GameState(
                "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");

        assertTrue(MoveValidator.isInCheck(state));
        assertEquals(0, Engine.bestMoves(state, 5, 1).length);
    }

    @Test
    public void stalematedSideHasNoMoves() {
        GameState state = new GameState(
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");

        assertFalse(MoveValidator.isInCheck(state));
        assertEquals(0, Engine.bestMoves(state, 5, 1).length);
    }

    @Test
    public void rejectsInvalidArguments() {
        GameState state = startingPosition();

        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(null, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(state, -1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(state, 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(state, 1, -1));
        assertThrows(IllegalArgumentException.class,
                () -> Engine.evaluate(null, true));
    }

    // utilities below

    private static GameState startingPosition() {
        return new GameState(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    private static EngineMove bestMove(GameState state, int depth) {
        EngineMove[] moves = Engine.bestMoves(state, 1, depth);

        assertEquals("Expected one best move", 1, moves.length);
        assertTrue(MoveValidator.isLegalMove(state, moves[0].move()));

        return moves[0];
    }

    private static void assertBestMoveAmong(
            GameState state,
            int depth,
            String... expectedMoves) {

        String actual = bestMove(state, depth).move().getUciMoveString();

        assertTrue(
                "Expected one of " + Arrays.toString(expectedMoves)
                        + ", but got " + actual,
                Arrays.asList(expectedMoves).contains(actual));
    }

    /**
     * On our turn, use the engine's selected move.
     * On the opponent's turn, verify every legal defense.
     */
    private static void assertEngineForcesMate(
            GameState state,
            boolean attackingSideIsWhite,
            int remainingDepth) {

        Set<Move> legalMoves = MoveValidator.getLegalMoves(state);

        if (legalMoves.isEmpty()) {
            assertTrue("Reached stalemate instead of checkmate",
                    MoveValidator.isInCheck(state));
            assertNotEquals(
                    "The attacking side was checkmated",
                    attackingSideIsWhite,
                    state.isWhiteTurn());
            return;
        }

        assertTrue("Engine did not force mate within the required depth",
                remainingDepth > 0);

        if (state.isWhiteTurn() == attackingSideIsWhite) {
            Move chosen = bestMove(state, remainingDepth).move();

            assertEngineForcesMate(
                    MoveHandler.applyMove(state, chosen),
                    attackingSideIsWhite,
                    remainingDepth - 1);
        } else {
            // mate must work against every defense
            for (Move reply : legalMoves) {
                assertEngineForcesMate(
                        MoveHandler.applyMove(state, reply),
                        attackingSideIsWhite,
                        remainingDepth - 1);
            }
        }
    }

    /**
     * Checks whether any attacking continuation forces mate.
     * Used to verify that a fixture really needs the stated depth.
     */
    private static boolean canForceMate(
            GameState state,
            boolean attackingSideIsWhite,
            int remainingDepth) {

        Set<Move> legalMoves = MoveValidator.getLegalMoves(state);

        if (legalMoves.isEmpty()) {
            return state.isWhiteTurn() != attackingSideIsWhite
                    && MoveValidator.isInCheck(state);
        }

        if (remainingDepth == 0) {
            return false;
        }

        if (state.isWhiteTurn() == attackingSideIsWhite) {
            // we only need one winning continuation
            for (Move move : legalMoves) {
                if (canForceMate(
                        MoveHandler.applyMove(state, move),
                        attackingSideIsWhite,
                        remainingDepth - 1)) {
                    return true;
                }
            }
            return false;
        }

        // every opponent reply must still lead to mate
        for (Move reply : legalMoves) {
            if (!canForceMate(
                    MoveHandler.applyMove(state, reply),
                    attackingSideIsWhite,
                    remainingDepth - 1)) {
                return false;
            }
        }
        return true;
    }
}
