package com.github.lfbr0.engine;

import static org.junit.Assert.*;

import com.github.lfbr0.board.ChessPiece;
import com.github.lfbr0.board.piece.*;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** Tests index-based moves, UCI parsing, serialization, and value equality. */
public class ChessMoveTests {

    @Test
    public void parsesNormalMove() {
        ChessMove move = new ChessMove("e2e4");
        assertEquals(12, move.getFrom());
        assertEquals(28, move.getTo());
        assertFalse(move.getPromotionPieceClass().isPresent());
    }

    @Test
    public void parsesCornerSquares() {
        ChessMove move = new ChessMove("a1h8");
        assertEquals(0, move.getFrom());
        assertEquals(63, move.getTo());
        ChessMove reverse = new ChessMove("h8a1");
        assertEquals(63, reverse.getFrom());
        assertEquals(0, reverse.getTo());
    }

    @Test
    public void parsesQueenPromotion() {
        assertPromotion("a7a8q", 48, 56, QueenPiece.class);
    }

    @Test
    public void parsesRookPromotion() {
        assertPromotion("a7a8r", 48, 56, RookPiece.class);
    }

    @Test
    public void parsesBishopPromotion() {
        assertPromotion("a7a8b", 48, 56, BishopPiece.class);
    }

    @Test
    public void parsesKnightPromotion() {
        assertPromotion("a7a8n", 48, 56, KnightPiece.class);
    }

    @Test
    public void parsesPromotionTowardsFirstRank() {
        assertPromotion("h2h1q", 15, 7, QueenPiece.class);
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
            assertEquals(input, new ChessMove(input).getUciMoveString());
        }
    }

    @Test
    public void parsesCastlingAsKingMovement() {
        assertCoordinates("e1g1", 4, 6);
        assertCoordinates("e1c1", 4, 2);
        assertCoordinates("e8g8", 60, 62);
        assertCoordinates("e8c8", 60, 58);
    }

    @Test
    public void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new ChessMove(null));
    }

    @Test
    public void rejectsEmptyOrBlankInput() {
        assertInvalidMoves("", " ", "   ", "\t", "\n");
    }

    @Test
    public void rejectsInvalidLengths() {
        assertInvalidMoves("e", "e2", "e2e", "e2e4qq", "e2e4e5");
    }

    @Test
    public void rejectsInvalidFiles() {
        assertInvalidMoves("i2e4", "e2i4", "z1a1", "a1z1");
    }

    @Test
    public void rejectsInvalidRanks() {
        assertInvalidMoves("e0e4", "e9e4", "e2e0", "e2e9");
    }

    @Test
    public void rejectsUppercaseCoordinates() {
        assertInvalidMoves("E2e4", "e2E4", "E2E4");
    }

    @Test
    public void rejectsInvalidPromotionSymbols() {
        assertInvalidMoves("a7a8k", "a7a8p", "a7a8x", "a7a81",
                "a7a8Q", "a7a8R", "a7a8B", "a7a8N");
    }

    @Test
    public void rejectsWhitespaceAroundOrInsideMove() {
        assertInvalidMoves(" e2e4", "e2e4 ", "e2 e4", "e2\te4",
                "e2e4\n", "e2e4\r\n");
    }

    @Test
    public void rejectsOtherMoveNotations() {
        assertInvalidMoves("e4", "Nf3", "O-O", "O-O-O",
                "e2-e4", "e2xe4", "a8=Q");
    }

    @Test
    public void rejectsNonAsciiCoordinates() {
        assertInvalidMoves("e\u0662e4", "e2e\uFF14", "\uFF45" + "2e4");
    }

    @Test
    public void parserDoesNotValidateBoardDependentLegality() {
        assertCoordinates("a1a8", 0, 56);
    }

    @Test
    public void sameSquareMoveIsRepresentableButNotNecessarilyLegal() {
        assertCoordinates("e4e4", 28, 28);
        assertEquals(new ChessMove("e4e4"), new ChessMove(28, 28));
    }

    @Test
    public void promotionLegalityBelongsToValidator() {
        assertEquals("e2e4q", new ChessMove("e2e4q").getUciMoveString());
        assertEquals(new ChessMove("e2e4q"), new ChessMove(12, 28, QueenPiece.class));
    }

    @Test
    public void convertsKnownSquaresToIndices() {
        assertEquals(0, ChessMove.squareStringToIndex("a1"));
        assertEquals(7, ChessMove.squareStringToIndex("h1"));
        assertEquals(8, ChessMove.squareStringToIndex("a2"));
        assertEquals(27, ChessMove.squareStringToIndex("d4"));
        assertEquals(56, ChessMove.squareStringToIndex("a8"));
        assertEquals(63, ChessMove.squareStringToIndex("h8"));
    }

    @Test
    public void convertsKnownIndicesToSquares() {
        assertEquals("a1", ChessMove.indexToSquareString(0));
        assertEquals("h1", ChessMove.indexToSquareString(7));
        assertEquals("a2", ChessMove.indexToSquareString(8));
        assertEquals("d4", ChessMove.indexToSquareString(27));
        assertEquals("a8", ChessMove.indexToSquareString(56));
        assertEquals("h8", ChessMove.indexToSquareString(63));
    }

    @Test
    public void convertsAllSixtyFourSquaresWithExpectedOrientation() {
        String[] ranks = {
                "a1 b1 c1 d1 e1 f1 g1 h1",
                "a2 b2 c2 d2 e2 f2 g2 h2",
                "a3 b3 c3 d3 e3 f3 g3 h3",
                "a4 b4 c4 d4 e4 f4 g4 h4",
                "a5 b5 c5 d5 e5 f5 g5 h5",
                "a6 b6 c6 d6 e6 f6 g6 h6",
                "a7 b7 c7 d7 e7 f7 g7 h7",
                "a8 b8 c8 d8 e8 f8 g8 h8"
        };
        int index = 0;
        for (String rank : ranks) {
            for (String square : rank.split(" ")) {
                assertEquals(square, index, ChessMove.squareStringToIndex(square));
                assertEquals("Index " + index, square, ChessMove.indexToSquareString(index));
                index++;
            }
        }
    }

    @Test
    public void rejectsInvalidSquareStrings() {
        for (String square : new String[] {
                null, "", " ", "a", "a10", "a0", "a9", "i1",
                "A1", "1a", " a1", "a1 ", "a1\n", "a\u0661"
        }) {
            assertThrows("Invalid square [" + square + "]",
                    IllegalArgumentException.class,
                    () -> ChessMove.squareStringToIndex(square));
        }
    }

    @Test
    public void rejectsOutOfRangeIndicesDuringConversion() {
        for (int index : invalidIndices()) {
            assertThrows("Index " + index, IllegalArgumentException.class,
                    () -> ChessMove.indexToSquareString(index));
        }
    }

    @Test
    public void constructsMoveFromIndices() {
        ChessMove move = new ChessMove(12, 28);
        assertEquals(12, move.getFrom());
        assertEquals(28, move.getTo());
        assertFalse(move.getPromotionPieceClass().isPresent());
        assertEquals("e2e4", move.getUciMoveString());
    }

    @Test
    public void indexConstructorSupportsCornerBoundaries() {
        assertEquals("a1h8", new ChessMove(0, 63).getUciMoveString());
        assertEquals("h8a1", new ChessMove(63, 0).getUciMoveString());
    }

    @Test
    public void indexConstructorSupportsEveryPromotion() {
        assertIndexPromotion(QueenPiece.class, "q");
        assertIndexPromotion(RookPiece.class, "r");
        assertIndexPromotion(BishopPiece.class, "b");
        assertIndexPromotion(KnightPiece.class, "n");
    }

    @Test
    public void nullPromotionMeansNoPromotion() {
        ChessMove move = new ChessMove(12, 28, null);
        assertEquals(new ChessMove(12, 28), move);
        assertFalse(move.getPromotionPieceClass().isPresent());
        assertEquals("e2e4", move.getUciMoveString());
    }

    @Test
    public void indexConstructorsRejectInvalidOrigins() {
        for (int index : invalidIndices()) {
            assertThrows(IllegalArgumentException.class, () -> new ChessMove(index, 28));
            assertThrows(IllegalArgumentException.class,
                    () -> new ChessMove(index, 56, QueenPiece.class));
        }
    }

    @Test
    public void indexConstructorsRejectInvalidDestinations() {
        for (int index : invalidIndices()) {
            assertThrows(IllegalArgumentException.class, () -> new ChessMove(12, index));
            assertThrows(IllegalArgumentException.class,
                    () -> new ChessMove(48, index, QueenPiece.class));
        }
    }

    @Test
    public void rejectsPawnOrKingAsPromotion() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChessMove(48, 56, PawnPiece.class));
        assertThrows(IllegalArgumentException.class,
                () -> new ChessMove(48, 56, KingPiece.class));
    }

    @Test
    public void allSquarePairsAgreeAcrossConstructorsAndSerialization() {
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                ChessMove indexed = new ChessMove(from, to);
                String uci = indexed.getUciMoveString();
                ChessMove parsed = new ChessMove(uci);
                assertEquals("Origin of " + uci, from, parsed.getFrom());
                assertEquals("Destination of " + uci, to, parsed.getTo());
                assertEquals(indexed, parsed);
                assertEquals(indexed.hashCode(), parsed.hashCode());
                assertEquals(uci, parsed.getUciMoveString());
            }
        }
    }

    @Test
    public void equalMovesHaveEqualValues() {
        ChessMove first = new ChessMove("e2e4");
        ChessMove second = new ChessMove(12, 28);
        ChessMove third = new ChessMove(12, 28, null);
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(second.hashCode(), third.hashCode());
    }

    @Test
    public void equalPromotionsHaveEqualValues() {
        ChessMove first = new ChessMove("a7a8n");
        ChessMove second = new ChessMove(48, 56, KnightPiece.class);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void movesWithDifferentOriginsAreNotEqual() {
        assertNotEquals(new ChessMove("e2e4"), new ChessMove("e3e4"));
    }

    @Test
    public void movesWithDifferentDestinationsAreNotEqual() {
        assertNotEquals(new ChessMove("e2e4"), new ChessMove("e2e3"));
    }

    @Test
    public void reversedMovesAreNotEqual() {
        assertNotEquals(new ChessMove(0, 63), new ChessMove(63, 0));
    }

    @Test
    public void allPromotionChoicesAreDistinctValues() {
        Set<ChessMove> moves = new HashSet<>();
        for (String suffix : new String[] {"", "q", "r", "b", "n"}) {
            moves.add(new ChessMove("a7a8" + suffix));
        }
        assertEquals(5, moves.size());
    }

    @Test
    public void promotionAndNonPromotionMovesAreNotEqual() {
        assertNotEquals(new ChessMove("a7a8q"), new ChessMove("a7a8"));
    }

    @Test
    public void moveIsNotEqualToNullOrAnotherType() {
        ChessMove move = new ChessMove("e2e4");
        assertFalse(move.equals(null));
        assertFalse(move.equals("e2e4"));
    }

    @Test
    public void equalMovesWorkAsSetKeys() {
        Set<ChessMove> moves = new HashSet<>();
        moves.add(new ChessMove("e2e4"));
        moves.add(new ChessMove(12, 28));
        assertEquals(1, moves.size());
        assertTrue(moves.contains(new ChessMove("e2e4")));
        assertFalse(moves.contains(new ChessMove("e2e3")));
    }

    private static void assertCoordinates(String input, int from, int to) {
        ChessMove move = new ChessMove(input);
        assertEquals(from, move.getFrom());
        assertEquals(to, move.getTo());
        assertFalse(move.getPromotionPieceClass().isPresent());
        assertEquals(input, move.getUciMoveString());
    }

    private static void assertPromotion(
            String input, int from, int to, Class<? extends ChessPiece> type) {
        ChessMove move = new ChessMove(input);
        assertEquals(from, move.getFrom());
        assertEquals(to, move.getTo());
        assertEquals(type, move.getPromotionPieceClass().orElseThrow());
        assertEquals(input, move.getUciMoveString());
    }

    private static void assertIndexPromotion(Class<? extends ChessPiece> type, String suffix) {
        ChessMove move = new ChessMove(48, 56, type);
        assertEquals(type, move.getPromotionPieceClass().orElseThrow());
        assertEquals("a7a8" + suffix, move.getUciMoveString());
        assertEquals(new ChessMove("a7a8" + suffix), move);
    }

    private static int[] invalidIndices() {
        return new int[] {Integer.MIN_VALUE, -8, -1, 64, 65, Integer.MAX_VALUE};
    }

    private static void assertInvalidMoves(String... inputs) {
        for (String input : inputs) {
            assertThrows("Expected rejection of [" + input + "]",
                    IllegalArgumentException.class, () -> new ChessMove(input));
        }
    }
}