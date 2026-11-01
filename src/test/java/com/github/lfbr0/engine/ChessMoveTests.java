package com.github.lfbr0.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.github.lfbr0.board.ChessPiece;
import com.github.lfbr0.board.piece.BishopPiece;
import com.github.lfbr0.board.piece.KnightPiece;
import com.github.lfbr0.board.piece.QueenPiece;
import com.github.lfbr0.board.piece.RookPiece;
import org.junit.Test;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/1/26
 * @project regis
 */
public class ChessMoveTests {

    @Test
    public void parsesNormalMove() {
        ChessMove move = new ChessMove("e2e4");

        assertEquals("e2", move.getFrom());
        assertEquals("e4", move.getTo());
        assertFalse(move.getPromotionPieceClass().isPresent());
    }

    @Test
    public void parsesCornerSquares() {
        ChessMove move = new ChessMove("a1h8");

        assertEquals("a1", move.getFrom());
        assertEquals("h8", move.getTo());
    }

    @Test
    public void parsesQueenPromotion() {
        assertPromotion("a7a8q", QueenPiece.class);
    }

    @Test
    public void parsesRookPromotion() {
        assertPromotion("a7a8r", RookPiece.class);
    }

    @Test
    public void parsesBishopPromotion() {
        assertPromotion("a7a8b", BishopPiece.class);
    }

    @Test
    public void parsesKnightPromotion() {
        assertPromotion("a7a8n", KnightPiece.class);
    }

    @Test
    public void parsesPromotionTowardsFirstRank() {
        ChessMove move = new ChessMove("h2h1q");

        assertEquals("h2", move.getFrom());
        assertEquals("h1", move.getTo());
        assertEquals(
                QueenPiece.class,
                move.getPromotionPieceClass().orElseThrow());
    }

    @Test
    public void serializesNormalMove() {
        assertEquals("e2e4", new ChessMove("e2e4").getUciMoveString());
    }

    @Test
    public void serializesEveryPromotionWithSingleLowercaseLetter() {
        for (String input : new String[] {
                "a7a8q", "a7a8r", "a7a8b", "a7a8n",
                "h2h1q", "h2h1r", "h2h1b", "h2h1n"
        }) {
            assertEquals(
                    "Serialization of " + input,
                    input,
                    new ChessMove(input).getUciMoveString());
        }
    }

    @Test
    public void parsesCastlingAsKingMovement() {
        // Standard chess castling coordinates.
        for (String input : new String[] {
                "e1g1", "e1c1", "e8g8", "e8c8"
        }) {
            ChessMove move = new ChessMove(input);

            assertEquals(input.substring(0, 2), move.getFrom());
            assertEquals(input.substring(2, 4), move.getTo());
            assertFalse(move.getPromotionPieceClass().isPresent());
            assertEquals(input, move.getUciMoveString());
        }
    }

    @Test
    public void rejectsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ChessMove(null));
    }

    @Test
    public void rejectsEmptyOrBlankInput() {
        assertInvalidMoves("", " ", "   ", "\t", "\n");
    }

    @Test
    public void rejectsInvalidLengths() {
        assertInvalidMoves(
                "e",
                "e2",
                "e2e",
                "e2e4qq",
                "e2e4e5");
    }

    @Test
    public void rejectsInvalidFiles() {
        assertInvalidMoves(
                "i2e4",
                "e2i4",
                "z1a1",
                "a1z1");
    }

    @Test
    public void rejectsInvalidRanks() {
        assertInvalidMoves(
                "e0e4",
                "e9e4",
                "e2e0",
                "e2e9");
    }

    @Test
    public void rejectsUppercaseCoordinates() {
        assertInvalidMoves("E2e4", "e2E4", "E2E4");
    }

    @Test
    public void rejectsInvalidPromotionSymbols() {
        assertInvalidMoves(
                "a7a8k",
                "a7a8p",
                "a7a8x",
                "a7a81",
                "a7a8Q",
                "a7a8R",
                "a7a8B",
                "a7a8N");
    }

    @Test
    public void rejectsWhitespaceAroundOrInsideMove() {
        assertInvalidMoves(
                " e2e4",
                "e2e4 ",
                "e2 e4",
                "e2\te4",
                "e2e4\n");
    }

    @Test
    public void rejectsOtherMoveNotations() {
        assertInvalidMoves(
                "e4",
                "Nf3",
                "O-O",
                "O-O-O",
                "e2-e4",
                "e2xe4",
                "a8=Q");
    }

    @Test
    public void doesNotValidateBoardDependentLegality() {
        // Parsing coordinates does not require a board.
        ChessMove move = new ChessMove("a1a8");

        assertEquals("a1", move.getFrom());
        assertEquals("a8", move.getTo());
    }

    @Test
    public void equalMovesHaveEqualValues() {
        ChessMove first = new ChessMove("e2e4");
        ChessMove second = new ChessMove("e2e4");
        ChessMove third = new ChessMove("e2e4");

        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void equalPromotionsHaveEqualValues() {
        ChessMove first = new ChessMove("a7a8n");
        ChessMove second = new ChessMove("a7a8n");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void movesWithDifferentOriginsAreNotEqual() {
        assertNotEquals(
                new ChessMove("e2e4"),
                new ChessMove("e3e4"));
    }

    @Test
    public void movesWithDifferentDestinationsAreNotEqual() {
        assertNotEquals(
                new ChessMove("e2e4"),
                new ChessMove("e2e3"));
    }

    @Test
    public void movesWithDifferentPromotionsAreNotEqual() {
        assertNotEquals(
                new ChessMove("a7a8q"),
                new ChessMove("a7a8n"));
    }

    @Test
    public void promotionAndNonPromotionMovesAreNotEqual() {
        assertNotEquals(
                new ChessMove("a7a8q"),
                new ChessMove("a7a8"));
    }

    @Test
    public void moveIsNotEqualToNullOrAnotherType() {
        ChessMove move = new ChessMove("e2e4");

        assertFalse(move.equals(null));
        assertFalse(move.equals("e2e4"));
    }

    private static void assertPromotion(
            String input,
            Class<? extends ChessPiece> expectedType) {

        ChessMove move = new ChessMove(input);

        assertEquals(input.substring(0, 2), move.getFrom());
        assertEquals(input.substring(2, 4), move.getTo());
        assertEquals(expectedType, move.getPromotionPieceClass().orElseThrow());
    }

    private static void assertInvalidMoves(String... inputs) {
        for (String input : inputs) {
            assertThrows(
                    "Expected rejection of [" + input + "]",
                    IllegalArgumentException.class,
                    () -> new ChessMove(input));
        }
    }
}