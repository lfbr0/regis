package com.github.lfbr0.engine;

import static org.junit.Assert.*;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.*;
import org.junit.Test;

/**
 * Tests applying already valid moves, without calling MoveValidator.
 * Fixtures use a1 = 0 and h8 = 63.
 *
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 */
public class MoveHandlerTests {

    @Test
    public void whiteKnightMovesAndClearsOrigin() {
        GameState state = position(true, "-", null,
                3, 10, "Ke1 ke8 Ng1");
        assertTransition(state, "g1f3", "Ke1 ke8 Nf3", "-",
                null, 4, 10);
    }

    @Test
    public void blackKnightMovesAndIncrementsFullmoveNumber() {
        GameState state = position(false, "-", null,
                3, 10, "Ke1 ke8 ng8");
        assertTransition(state, "g8f6", "Ke1 ke8 nf6", "-",
                null, 4, 11);
    }

    @Test
    public void bishopMovesDiagonally() {
        GameState state = position(true, "-", null,
                5, 7, "Ke1 ke8 Bc1");
        assertTransition(state, "c1g5", "Ke1 ke8 Bg5", "-",
                null, 6, 7);
    }

    @Test
    public void queenMovesAlongRank() {
        GameState state = position(true, "-", null,
                5, 7, "Ke1 ke8 Qd4");
        assertTransition(state, "d4g4", "Ke1 ke8 Qg4", "-",
                null, 6, 7);
    }

    @Test
    public void queenMovesAlongFile() {
        GameState state = position(false, "-", null,
                5, 7, "Ke1 ke8 qd5");
        assertTransition(state, "d5d3", "Ke1 ke8 qd3", "-",
                null, 6, 8);
    }

    @Test
    public void whiteCaptureReplacesEnemyAndResetsClock() {
        GameState state = position(true, "-", null,
                18, 9, "Ke1 ke8 Bc4 nf7");
        assertTransition(state, "c4f7", "Ke1 ke8 Bf7", "-",
                null, 0, 9);
    }

    @Test
    public void blackCaptureReplacesEnemyAndResetsClock() {
        GameState state = position(false, "-", null,
                18, 9, "Ke1 ke8 bc5 Nf2");
        assertTransition(state, "c5f2", "Ke1 ke8 bf2", "-",
                null, 0, 10);
    }

    @Test
    public void ordinaryMoveClearsEnPassantTarget() {
        GameState state = position(true, "-", "d6",
                0, 12, "Ke1 ke8 Ng1 pd5");
        assertTransition(state, "g1f3", "Ke1 ke8 Nf3 pd5", "-",
                null, 1, 12);
    }

    @Test
    public void whiteSinglePawnAdvanceResetsClock() {
        GameState state = position(true, "-", null,
                22, 9, "Ke1 ke8 Pe2");
        assertTransition(state, "e2e3", "Ke1 ke8 Pe3", "-",
                null, 0, 9);
    }

    @Test
    public void blackSinglePawnAdvanceResetsClock() {
        GameState state = position(false, "-", null,
                22, 9, "Ke1 ke8 pe7");
        assertTransition(state, "e7e6", "Ke1 ke8 pe6", "-",
                null, 0, 10);
    }

    @Test
    public void whiteDoubleAdvanceSetsCrossedSquare() {
        GameState state = position(true, "-", null,
                22, 9, "Ke1 ke8 Pe2");
        assertTransition(state, "e2e4", "Ke1 ke8 Pe4", "-",
                "e3", 0, 9);
    }

    @Test
    public void blackDoubleAdvanceSetsCrossedSquare() {
        GameState state = position(false, "-", null,
                22, 9, "Ke1 ke8 pe7");
        assertTransition(state, "e7e5", "Ke1 ke8 pe5", "-",
                "e6", 0, 10);
    }

    @Test
    public void doubleAdvanceReplacesOldTarget() {
        GameState state = position(true, "-", "d6",
                0, 9, "Ke1 ke8 Pe2 pd5");
        assertTransition(state, "e2e4", "Ke1 ke8 Pe4 pd5", "-",
                "e3", 0, 9);
    }

    @Test
    public void singleAdvanceClearsOldTarget() {
        GameState state = position(true, "-", "d6",
                0, 9, "Ke1 ke8 Pe2 pd5");
        assertTransition(state, "e2e3", "Ke1 ke8 Pe3 pd5", "-",
                null, 0, 9);
    }

    @Test
    public void whitePawnCapture() {
        GameState state = position(true, "-", null,
                3, 9, "Ke1 ke8 Pe4 nd5");
        assertTransition(state, "e4d5", "Ke1 ke8 Pd5", "-",
                null, 0, 9);
    }

    @Test
    public void blackPawnCapture() {
        GameState state = position(false, "-", null,
                3, 9, "Ke1 ke8 pe5 Nd4");
        assertTransition(state, "e5d4", "Ke1 ke8 pd4", "-",
                null, 0, 10);
    }

    @Test
    public void whiteEnPassantFrome5Tod6() {
        GameState state = position(true, "-", "d6",
                0, 12, "Ka1 kh8 Pe5 pd5");
        assertTransition(state, "e5d6", "Ka1 kh8 Pd6", "-",
                null, 0, 12);
    }

    @Test
    public void whiteEnPassantFromd5Toe6() {
        GameState state = position(true, "-", "e6",
                0, 12, "Ka1 kh8 Pd5 pe5");
        assertTransition(state, "d5e6", "Ka1 kh8 Pe6", "-",
                null, 0, 12);
    }

    @Test
    public void blackEnPassantFrome4Tod3() {
        GameState state = position(false, "-", "d3",
                0, 12, "Ka1 kh8 pe4 Pd4");
        assertTransition(state, "e4d3", "Ka1 kh8 pd3", "-",
                null, 0, 13);
    }

    @Test
    public void blackEnPassantFromd4Toe3() {
        GameState state = position(false, "-", "e3",
                0, 12, "Ka1 kh8 pd4 Pe4");
        assertTransition(state, "d4e3", "Ka1 kh8 pe3", "-",
                null, 0, 13);
    }

    @Test
    public void whitePromotesToQueen() {
        GameState state = position(true, "-", null,
                0, 19, "Kh1 kh8 Pa7");
        assertTransition(state, "a7a8q", "Kh1 kh8 Qa8", "-",
                null, 0, 19);
    }

    @Test
    public void whitePromotesToRook() {
        GameState state = position(true, "-", null,
                0, 19, "Kh1 kh8 Pa7");
        assertTransition(state, "a7a8r", "Kh1 kh8 Ra8", "-",
                null, 0, 19);
    }

    @Test
    public void whitePromotesToBishop() {
        GameState state = position(true, "-", null,
                0, 19, "Kh1 kh8 Pa7");
        assertTransition(state, "a7a8b", "Kh1 kh8 Ba8", "-",
                null, 0, 19);
    }

    @Test
    public void whitePromotesToKnight() {
        GameState state = position(true, "-", null,
                0, 19, "Kh1 kh8 Pa7");
        assertTransition(state, "a7a8n", "Kh1 kh8 Na8", "-",
                null, 0, 19);
    }

    @Test
    public void blackPromotesToQueen() {
        GameState state = position(false, "-", null,
                0, 19, "Kh1 kh8 pa2");
        assertTransition(state, "a2a1q", "Kh1 kh8 qa1", "-",
                null, 0, 20);
    }

    @Test
    public void blackPromotesToRook() {
        GameState state = position(false, "-", null,
                0, 19, "Kh1 kh8 pa2");
        assertTransition(state, "a2a1r", "Kh1 kh8 ra1", "-",
                null, 0, 20);
    }

    @Test
    public void blackPromotesToBishop() {
        GameState state = position(false, "-", null,
                0, 19, "Kh1 kh8 pa2");
        assertTransition(state, "a2a1b", "Kh1 kh8 ba1", "-",
                null, 0, 20);
    }

    @Test
    public void blackPromotesToKnight() {
        GameState state = position(false, "-", null,
                0, 19, "Kh1 kh8 pa2");
        assertTransition(state, "a2a1n", "Kh1 kh8 na1", "-",
                null, 0, 20);
    }

    @Test
    public void whiteKingsideCastling() {
        GameState state = position(true, "KQkq", null,
                8, 15, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "e1g1", "Kg1 Rf1 Ra1 ke8 ra8 rh8", "kq",
                null, 9, 15);
    }

    @Test
    public void whiteQueensideCastling() {
        GameState state = position(true, "KQkq", null,
                8, 15, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "e1c1", "Kc1 Rd1 Rh1 ke8 ra8 rh8", "kq",
                null, 9, 15);
    }

    @Test
    public void blackKingsideCastling() {
        GameState state = position(false, "KQkq", null,
                8, 15, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "e8g8", "kg8 rf8 ra8 Ke1 Ra1 Rh1", "KQ",
                null, 9, 16);
    }

    @Test
    public void blackQueensideCastling() {
        GameState state = position(false, "KQkq", null,
                8, 15, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "e8c8", "kc8 rd8 rh8 Ke1 Ra1 Rh1", "KQ",
                null, 9, 16);
    }

    @Test
    public void ordinaryWhiteKingMoveRemovesBothRights() {
        GameState state = position(true, "KQkq", null,
                4, 5, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "e1e2", "Ke2 Ra1 Rh1 ke8 ra8 rh8", "kq",
                null, 5, 5);
    }

    @Test
    public void ordinaryBlackKingMoveRemovesBothRights() {
        GameState state = position(false, "KQkq", null,
                4, 5, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "e8e7", "Ke1 Ra1 Rh1 ke7 ra8 rh8", "KQ",
                null, 5, 6);
    }

    @Test
    public void kingMoveToG1DoesNotMoveRook() {
        GameState state = position(true, "-", null,
                4, 5, "Kf1 Rh1 ke8");
        assertTransition(state, "f1g1", "Kg1 Rh1 ke8", "-",
                null, 5, 5);
    }

    @Test
    public void kingMoveToC1DoesNotMoveRook() {
        GameState state = position(true, "-", null,
                4, 5, "Kd1 Ra1 ke8");
        assertTransition(state, "d1c1", "Kc1 Ra1 ke8", "-",
                null, 5, 5);
    }

    @Test
    public void blackKingMoveToG8DoesNotMoveRook() {
        GameState state = position(false, "-", null,
                4, 5, "Ke1 kf8 rh8");
        assertTransition(state, "f8g8", "Ke1 kg8 rh8", "-",
                null, 5, 6);
    }

    @Test
    public void whiteKingCaptureRemovesBlackRookRight() {
        GameState state = position(true, "q", null,
                7, 20, "Kb7 kh8 ra8");
        assertTransition(state, "b7a8", "Ka8 kh8", "-",
                null, 0, 20);
    }

    @Test
    public void blackKingCaptureRemovesWhiteRookRight() {
        GameState state = position(false, "Q", null,
                7, 20, "Kh1 kb2 Ra1");
        assertTransition(state, "b2a1", "Kh1 ka1", "-",
                null, 0, 21);
    }

    @Test
    public void rookLeavingA1RemovesOnlyItsRight() {
        GameState state = position(true, "KQkq", null,
                5, 12, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "a1a2", "Ke1 Ra2 Rh1 ke8 ra8 rh8", "Kkq",
                null, 6, 12);
    }

    @Test
    public void rookLeavingH1RemovesOnlyItsRight() {
        GameState state = position(true, "KQkq", null,
                5, 12, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "h1h2", "Ke1 Ra1 Rh2 ke8 ra8 rh8", "Qkq",
                null, 6, 12);
    }

    @Test
    public void rookLeavingA8RemovesOnlyItsRight() {
        GameState state = position(false, "KQkq", null,
                5, 12, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "a8a7", "Ke1 Ra1 Rh1 ke8 ra7 rh8", "KQk",
                null, 6, 13);
    }

    @Test
    public void rookLeavingH8RemovesOnlyItsRight() {
        GameState state = position(false, "KQkq", null,
                5, 12, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "h8h7", "Ke1 Ra1 Rh1 ke8 ra8 rh7", "KQq",
                null, 6, 13);
    }

    @Test
    public void rookAwayFromStartingSquarePreservesOtherRights() {
        GameState state = position(true, "Kkq", null,
                5, 12, "Ke1 Ra3 Rh1 ke8 ra8 rh8");
        assertTransition(state, "a3a4", "Ke1 Ra4 Rh1 ke8 ra8 rh8", "Kkq",
                null, 6, 12);
    }

    @Test
    public void rookReturningDoesNotRestoreRight() {
        GameState state = position(true, "K", null,
                5, 12, "Ke1 Ra2 Rh1 ke8");
        assertTransition(state, "a2a1", "Ke1 Ra1 Rh1 ke8", "K",
                null, 6, 12);
    }

    @Test
    public void kingReturningDoesNotRestoreRights() {
        GameState state = position(true, "-", null,
                5, 12, "Ke2 Ra1 Rh1 ke8");
        assertTransition(state, "e2e1", "Ke1 Ra1 Rh1 ke8", "-",
                null, 6, 12);
    }

    @Test
    public void rookCaptureCanRemoveRightsForBothSides() {
        GameState state = position(true, "KQkq", null,
                5, 12, "Ke1 Ra1 Rh1 ke8 ra8 rh8");
        assertTransition(state, "a1a8", "Ke1 Ra8 Rh1 ke8 rh8", "Kk",
                null, 0, 12);
    }

    @Test
    public void captureBlackKingsideRookRemovesOnlyItsRight() {
        GameState state = position(true, "kq", null,
                5, 12, "Ke1 Bg7 ke8 ra8 rh8");
        assertTransition(state, "g7h8", "Ke1 Bh8 ke8 ra8", "q",
                null, 0, 12);
    }

    @Test
    public void captureWhiteKingsideRookRemovesOnlyItsRight() {
        GameState state = position(false, "KQ", null,
                5, 12, "Ke1 Ra1 Rh1 ke8 bg2");
        assertTransition(state, "g2h1", "Ke1 Ra1 bh1 ke8", "Q",
                null, 0, 13);
    }

    @Test
    public void whiteCapturePromotionRemovesQueensideRookRight() {
        GameState state = position(true, "KQkq", null,
                0, 20, "Ke1 ke8 Pb7 ra8");
        assertTransition(state, "b7a8q", "Ke1 ke8 Qa8", "KQk",
                null, 0, 20);
    }

    @Test
    public void whiteCapturePromotionRemovesKingsideRookRight() {
        GameState state = position(true, "KQkq", null,
                0, 20, "Ke1 ke8 Pg7 rh8");
        assertTransition(state, "g7h8q", "Ke1 ke8 Qh8", "KQq",
                null, 0, 20);
    }

    @Test
    public void blackCapturePromotionRemovesQueensideRookRight() {
        GameState state = position(false, "KQkq", null,
                0, 20, "Ke1 ke8 pb2 Ra1");
        assertTransition(state, "b2a1q", "Ke1 ke8 qa1", "Kkq",
                null, 0, 21);
    }

    @Test
    public void blackCapturePromotionRemovesKingsideRookRight() {
        GameState state = position(false, "KQkq", null,
                0, 20, "Ke1 ke8 pg2 Rh1");
        assertTransition(state, "g2h1q", "Ke1 ke8 qh1", "Qkq",
                null, 0, 21);
    }
    @Test
    public void rejectsNullState() {
        assertThrows(IllegalArgumentException.class,
                () -> MoveHandler.applyMove(null, new Move("e2e4")));
    }

    @Test
    public void rejectsNullMove() {
        GameState state = position(true, "-", null, 0, 1, "Ke1 ke8 Pe2");
        assertThrows(IllegalArgumentException.class,
                () -> MoveHandler.applyMove(state, null));
    }

    @Test
    public void consecutiveMovesUpdateCountersOnceAndExpireEnPassant() {
        GameState start = position(true, "-", null, 8, 10, "Ke1 ke8 Pe2 ng8");
        GameState afterWhite = MoveHandler.applyMove(start, new Move("e2e4"));
        GameState afterBlack = MoveHandler.applyMove(afterWhite, new Move("g8f6"));

        assertBoard("Ke1 ke8 Pe4 nf6", afterBlack);
        assertTrue(afterBlack.isWhiteTurn());
        assertNull(afterBlack.getEnPassantSquare());
        assertEquals(1, afterBlack.getHalfmoveClock());
        assertEquals(11, afterBlack.getFullmoveNumber());

        // applying the next move must not change the previous state
        assertBoard("Ke1 ke8 Pe4 ng8", afterWhite);
        assertEquals("e3", afterWhite.getEnPassantSquare());
        assertEquals(0, afterWhite.getHalfmoveClock());
        assertFalse(afterWhite.isWhiteTurn());
    }

    @Test
    public void applyingSameMoveTwiceToOriginalProducesIndependentStates() {
        GameState start = position(true, "-", null, 3, 10, "Ke1 ke8 Ng1");
        GameState first = MoveHandler.applyMove(start, new Move("g1f3"));
        GameState second = MoveHandler.applyMove(start, new Move("g1f3"));
        assertNotSame(first, second);
        assertBoard("Ke1 ke8 Nf3", first);
        assertBoard("Ke1 ke8 Nf3", second);
        assertBoard("Ke1 ke8 Ng1", start);
    }

    private static void assertTransition(
            GameState original, String uci, String expectedPieces, String rights,
            String enPassant, int halfmove, int fullmove) {
        Piece[] before = original.getBoardCopy();
        boolean originalTurn = original.isWhiteTurn();
        String originalRights = rightsOf(original);
        String originalEnPassant = original.getEnPassantSquare();
        int originalHalfmove = original.getHalfmoveClock();
        int originalFullmove = original.getFullmoveNumber();

        GameState result = MoveHandler.applyMove(original, new Move(uci));

        assertNotNull(result);
        assertNotSame("Must return a new state", original, result);
        assertBoard(expectedPieces, result);
        assertEquals("Turn", !originalTurn, result.isWhiteTurn());
        assertEquals("Castling rights", rights, rightsOf(result));
        assertEquals("En passant target", enPassant, result.getEnPassantSquare());
        assertEquals("Halfmove clock", halfmove, result.getHalfmoveClock());
        assertEquals("Fullmove number", fullmove, result.getFullmoveNumber());

        // applying any move, including special moves, must preserve the input
        for (int i = 0; i < 64; i++) {
            assertSame("Original square " + i, before[i], original.getPieceAtIndex(i));
        }
        assertEquals(originalTurn, original.isWhiteTurn());
        assertEquals(originalRights, rightsOf(original));
        assertEquals(originalEnPassant, original.getEnPassantSquare());
        assertEquals(originalHalfmove, original.getHalfmoveClock());
        assertEquals(originalFullmove, original.getFullmoveNumber());
    }

    private static GameState position(
            boolean white, String rights, String ep,
            int halfmove, int fullmove, String pieces) {
        return new GameState(
                board(pieces), white,
                rights.contains("K"), rights.contains("Q"),
                rights.contains("k"), rights.contains("q"),
                ep, halfmove, fullmove);
    }

    // compare all 64 squares so duplicated pieces and unintended changes are caught
    private static void assertBoard(String pieces, GameState actual) {
        Piece[] expected = board(pieces);
        for (int i = 0; i < 64; i++) {
            Piece piece = actual.getPieceAtIndex(i);
            if (expected[i] == null) {
                assertNull("Expected empty square " + i, piece);
            } else {
                assertNotNull("Expected piece on square " + i, piece);
                assertEquals("Piece type on square " + i, expected[i].getClass(), piece.getClass());
                assertEquals("Piece color on square " + i, expected[i].isWhite(), piece.isWhite());
            }
        }
    }

    // fixture entries contain a FEN piece symbol and square, e.g. "Ke1" or "pd5"
    private static Piece[] board(String entries) {
        Piece[] board = new Piece[64];
        for (String entry : entries.split(" ")) {
            int square = (entry.charAt(2) - '1') * 8 + entry.charAt(1) - 'a';
            if (board[square] != null) {
                throw new IllegalArgumentException("Duplicate fixture square: " + entry);
            }
            boolean white = Character.isUpperCase(entry.charAt(0));
            board[square] = switch (Character.toUpperCase(entry.charAt(0))) {
                case 'P' -> new PawnPiece(white);
                case 'N' -> new KnightPiece(white);
                case 'B' -> new BishopPiece(white);
                case 'R' -> new RookPiece(white);
                case 'Q' -> new QueenPiece(white);
                case 'K' -> new KingPiece(white);
                default -> throw new IllegalArgumentException("Invalid fixture: " + entry);
            };
        }
        return board;
    }

    private static String rightsOf(GameState state) {
        String rights = "";
        if (state.isWhiteKingsideCastlingAllowed()) rights += "K";
        if (state.isWhiteQueensideCastlingAllowed()) rights += "Q";
        if (state.isBlackKingsideCastlingAllowed()) rights += "k";
        if (state.isBlackQueensideCastlingAllowed()) rights += "q";
        return rights.isEmpty() ? "-" : rights;
    }
}