package com.github.lfbr0.engine;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.move.Move;
import com.github.lfbr0.move.MoveHandler;
import com.github.lfbr0.move.MoveValidator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class EngineTests {

    private static final long SEARCH_TIMEOUT_MILLIS = 120_000;

    // material-winning moves: test the same idea for both colors

    @Test
    public void whiteRookCapturesFreeQueen() {
        assertCapture(false,
                "7k/8/8/8/8/8/q7/R6K w - - 0 1",
                "a1a2");
    }

    @Test
    public void blackRookCapturesFreeQueen() {
        assertCapture(true,
                "7k/8/8/8/8/8/q7/R6K w - - 0 1",
                "a1a2");
    }

    @Test
    public void whiteBishopCapturesFreeRook() {
        assertCapture(false,
                "7k/8/8/8/8/8/3r4/2B4K w - - 0 1",
                "c1d2");
    }

    @Test
    public void blackBishopCapturesFreeRook() {
        assertCapture(true,
                "7k/8/8/8/8/8/3r4/2B4K w - - 0 1",
                "c1d2");
    }

    @Test
    public void whiteKnightCapturesFreeQueen() {
        assertCapture(false,
                "7k/8/8/8/8/2q5/8/1N5K w - - 0 1",
                "b1c3");
    }

    @Test
    public void blackKnightCapturesFreeQueen() {
        assertCapture(true,
                "7k/8/8/8/8/2q5/8/1N5K w - - 0 1",
                "b1c3");
    }

    @Test
    public void whiteQueenCapturesFreeRook() {
        assertCapture(false,
                "7k/8/8/8/8/8/3r4/3Q3K w - - 0 1",
                "d1d2");
    }

    @Test
    public void blackQueenCapturesFreeRook() {
        assertCapture(true,
                "7k/8/8/8/8/8/3r4/3Q3K w - - 0 1",
                "d1d2");
    }

    @Test
    public void whitePawnCapturesFreeQueen() {
        assertCapture(false,
                "7k/8/8/3q4/4P3/8/8/7K w - - 0 1",
                "e4d5");
    }

    @Test
    public void blackPawnCapturesFreeQueen() {
        assertCapture(true,
                "7k/8/8/3q4/4P3/8/8/7K w - - 0 1",
                "e4d5");
    }

    // search must account for the opponent's reply

    @Test
    public void whiteAvoidsLosingQueenToRecapture() {
        assertAvoidsRecapture(false);
    }

    @Test
    public void blackAvoidsLosingQueenToRecapture() {
        assertAvoidsRecapture(true);
    }

    @Test
    public void whiteFindsKnightFork() {
        assertKnightFork(false);
    }

    @Test
    public void blackFindsKnightFork() {
        assertKnightFork(true);
    }

    // pins

    @Test
    public void whitePinnedRookCapturesAttacker() {
        assertCapture(false,
                "4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1",
                "e2e8");
    }

    @Test
    public void blackPinnedRookCapturesAttacker() {
        assertCapture(true,
                "4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1",
                "e2e8");
    }

    @Test
    public void whitePinnedRookStaysOnKingsFile() {
        assertPinnedRook(false);
    }

    @Test
    public void blackPinnedRookStaysOnKingsFile() {
        assertPinnedRook(true);
    }

    @Test
    public void whitePinnedPawnOnlyCapturesAttacker() {
        assertPinnedPawn(false);
    }

    @Test
    public void blackPinnedPawnOnlyCapturesAttacker() {
        assertPinnedPawn(true);
    }

    // mates: accept any move that forces mate within the required depth

    @Test
    public void whiteFindsMateInOne() {
        assertMate(false, 1,
                "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1");
    }

    @Test
    public void blackFindsMateInOne() {
        assertMate(true, 1,
                "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1");
    }

    @Test
    public void whiteFindsMateInTwo() {
        assertMate(false, 3,
                "7k/8/8/5K2/6Q1/8/8/8 w - - 0 1");
    }

    @Test
    public void blackFindsMateInTwo() {
        assertMate(true, 3,
                "7k/8/8/5K2/6Q1/8/8/8 w - - 0 1");
    }

    @Test
    public void whiteFindsMateInThree() {
        assertMate(false, 5,
                "7k/8/8/8/5K2/6Q1/8/8 w - - 0 1");
    }

    @Test
    public void blackFindsMateInThree() {
        assertMate(true, 5,
                "7k/8/8/8/5K2/6Q1/8/8 w - - 0 1");
    }

    @Test
    public void whiteAcceptsAnyOfMultipleMates() {
        assertMultipleMates(false);
    }

    @Test
    public void blackAcceptsAnyOfMultipleMates() {
        assertMultipleMates(true);
    }

    // terminal positions

    @Test
    public void checkmatedBlackHasNoMoves() {
        assertTerminal(false, true,
                "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
    }

    @Test
    public void checkmatedWhiteHasNoMoves() {
        assertTerminal(true, true,
                "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
    }

    @Test
    public void stalematedBlackHasNoMoves() {
        assertTerminal(false, false,
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
    }

    @Test
    public void stalematedWhiteHasNoMoves() {
        assertTerminal(true, false,
                "7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
    }

    // special moves

    @Test
    public void whiteChoosesPromotion() {
        assertPromotion(false);
    }

    @Test
    public void blackChoosesPromotion() {
        assertPromotion(true);
    }

    @Test
    public void whiteChoosesCapturePromotion() {
        assertCapturePromotion(false);
    }

    @Test
    public void blackChoosesCapturePromotion() {
        assertCapturePromotion(true);
    }

    @Test
    public void whiteCapturesEnPassant() {
        assertEnPassant(false);
    }

    @Test
    public void blackCapturesEnPassant() {
        assertEnPassant(true);
    }

    @Test
    public void includesBothCastlingMovesWhenLegal() {
        GameState state = position(
                "4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");

        Set<String> moves = uciMoves(search(state, 100, 1));

        assertTrue(moves.contains("e1g1"));
        assertTrue(moves.contains("e1c1"));
    }

    @Test
    public void excludesCastlingWhenRightsAreAbsent() {
        GameState state = position(
                "4k3/8/8/8/8/8/8/R3K2R w - - 0 1");

        Set<String> moves = uciMoves(search(state, 100, 1));

        assertFalse(moves.contains("e1g1"));
        assertFalse(moves.contains("e1c1"));
    }

    // returned moves and counts

    @Test
    public void returnsAllTwentyStartingMoves() {
        GameState state = startingPosition();

        EngineMove[] results = search(state, 100, 1);

        assertEquals(20, results.length);
        assertLegalDistinct(state, results);

        Set<Move> actual = new HashSet<>();
        for (EngineMove result : results) {
            actual.add(result.move());
        }

        assertEquals(MoveValidator.getLegalMoves(state), actual);
    }

    @Test
    public void respectsRequestedCount() {
        assertEquals(3, search(startingPosition(), 3, 1).length);
    }

    @Test
    public void zeroCountReturnsNothing() {
        assertEquals(0, search(startingPosition(), 0, 1).length);
    }

    @Test
    public void tinyTimeoutReturnsOnlyLegalCompletedMoves() {
        GameState state = startingPosition();

        EngineMove[] results =
                Engine.bestMoves(state, 5, 10, 1);

        // no evaluator may finish before this deadline
        assertTrue(results.length <= 5);
        assertLegalDistinct(state, results);
    }

    // preservation of the input position

    @Test
    public void ordinarySearchPreservesState() {
        assertSearchPreservesState(
                "7k/8/8/8/8/8/q7/R6K w - - 17 42");
    }

    @Test
    public void castlingSearchPreservesState() {
        assertSearchPreservesState(
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 8 12");
    }

    @Test
    public void enPassantSearchPreservesState() {
        assertSearchPreservesState(
                "7k/8/8/3pP3/8/8/8/7K w - d6 0 23");
    }

    @Test
    public void promotionSearchPreservesState() {
        assertSearchPreservesState(
                "8/P6k/8/8/8/8/8/K7 w - - 0 41");
    }

    // signals must belong to one search only

    @Test
    public void mateSignalDoesNotStopFollowingSearch() {
        assertMate(false, 1,
                "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1");

        assertCapture(false,
                "7k/8/8/8/8/8/q7/R6K w - - 0 1",
                "a1a2");
    }

    @Test
    public void timeoutDoesNotStopFollowingSearch() {
        Engine.bestMoves(startingPosition(), 5, 10, 1);

        assertCapture(false,
                "7k/8/8/8/8/8/q7/R6K w - - 0 1",
                "a1a2");
    }

    @Test
    public void concurrentSearchesDoNotMixResults() throws Exception {
        ExecutorService callers = Executors.newFixedThreadPool(2);

        try {
            Future<Move> white = callers.submit(() ->
                    bestMove(position(
                            "7k/8/8/8/8/8/q7/R6K w - - 0 1"), 1));

            Future<Move> black = callers.submit(() ->
                    bestMove(position(
                            "r6k/Q7/8/8/8/8/8/7K b - - 0 1"), 1));

            assertEquals("a1a2",
                    white.get(130, TimeUnit.SECONDS).getUciMoveString());
            assertEquals("a8a7",
                    black.get(130, TimeUnit.SECONDS).getUciMoveString());
        } finally {
            callers.shutdownNow();
        }
    }

    @Test
    public void concurrentMateSignalDoesNotCancelOtherSearch()
            throws Exception {

        ExecutorService callers = Executors.newFixedThreadPool(2);

        try {
            Future<Move> mating = callers.submit(() ->
                    bestMove(position(
                            "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1"), 1));

            Future<Move> capture = callers.submit(() ->
                    bestMove(position(
                            "7k/8/8/8/8/8/q7/R6K w - - 0 1"), 1));

            GameState mateState = position(
                    "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1");

            Move matingMove = mating.get(130, TimeUnit.SECONDS);

            assertTrue(immediateMatingMoves(mateState)
                    .contains(matingMove));

            assertEquals("a1a2",
                    capture.get(130, TimeUnit.SECONDS).getUciMoveString());
        } finally {
            callers.shutdownNow();
        }
    }

    // arguments

    @Test
    public void rejectsNullState() {
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(null, 1, 1, 1_000));
    }

    @Test
    public void rejectsNegativeCount() {
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(startingPosition(), -1, 1, 1_000));
    }

    @Test
    public void rejectsZeroDepth() {
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(startingPosition(), 1, 0, 1_000));
    }

    @Test
    public void rejectsNegativeDepth() {
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(startingPosition(), 1, -1, 1_000));
    }

    @Test
    public void rejectsZeroTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(startingPosition(), 1, 1, 0));
    }

    @Test
    public void rejectsNegativeTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(startingPosition(), 1, 1, -1));
    }

    @Test
    public void rejectsOverflowingTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> Engine.bestMoves(
                        startingPosition(), 1, 1, Long.MAX_VALUE));
    }

    @Test
    public void evaluationRejectsNullState() {
        assertThrows(IllegalArgumentException.class,
                () -> Engine.evaluate(null, true));
    }

    // utilities below

    private static GameState position(String fen) {
        return new GameState(fen);
    }

    private static GameState startingPosition() {
        return position(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
                        + " w KQkq - 0 1");
    }

    private static EngineMove[] search(
            GameState state,
            int count,
            int depth) {

        return Engine.bestMoves(
                state, count, depth, SEARCH_TIMEOUT_MILLIS);
    }

    private static Move bestMove(GameState state, int depth) {
        EngineMove[] results = search(state, 1, depth);

        assertEquals(
                "Expected one result; the search may have timed out",
                1,
                results.length);

        assertTrue(
                "Engine returned an illegal move",
                MoveValidator.isLegalMove(state, results[0].move()));

        return results[0].move();
    }

    private static void assertBestMoveAmong(
            GameState state,
            int depth,
            String... acceptableMoves) {

        String actual = bestMove(state, depth).getUciMoveString();

        assertTrue(
                "Unexpected best move: " + actual,
                Set.of(acceptableMoves).contains(actual));
    }

    private static void assertCapture(
            boolean mirrored,
            String fen,
            String expectedMove) {

        assertBestMoveAmong(
                fixture(fen, mirrored),
                1,
                mirrored ? mirrorUci(expectedMove) : expectedMove);
    }

    private static void assertAvoidsRecapture(boolean mirrored) {
        GameState state = fixture(
                "r6k/8/8/8/8/8/q7/Q6K w - - 0 1",
                mirrored);

        String losingCapture = mirrored
                ? mirrorUci("a1a2")
                : "a1a2";

        assertNotEquals(
                losingCapture,
                bestMove(state, 2).getUciMoveString());
    }

    private static void assertKnightFork(boolean mirrored) {
        GameState state = fixture(
                "2q3k1/8/8/3N4/8/8/8/K7 w - - 0 1",
                mirrored);

        String fork = mirrored ? mirrorUci("d5e7") : "d5e7";
        String capture = mirrored ? mirrorUci("e7c8") : "e7c8";

        assertBestMoveAmong(state, 3, fork);

        GameState forked =
                MoveHandler.applyMove(state, new Move(fork));

        assertTrue(MoveValidator.isInCheck(forked));

        Set<Move> replies = MoveValidator.getLegalMoves(forked);
        assertFalse(replies.isEmpty());

        // after every defense, the queen must still be available
        for (Move reply : replies) {
            assertBestMoveAmong(
                    MoveHandler.applyMove(forked, reply),
                    1,
                    capture);
        }
    }

    private static void assertPinnedRook(boolean mirrored) {
        GameState state = fixture(
                "4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1",
                mirrored);

        int origin = Move.squareStringToIndex(
                mirrored ? "e7" : "e2");

        boolean found = false;

        for (EngineMove result : search(state, 100, 1)) {
            Move move = result.move();

            if (move.getFrom() == origin) {
                found = true;

                // moving sideways exposes the king
                assertEquals(4, move.getTo() % 8);
            }
        }

        assertTrue("Expected a move by the pinned rook", found);
    }

    private static void assertPinnedPawn(boolean mirrored) {
        GameState state = fixture(
                "7k/8/8/2n1b3/3P4/8/8/K7 w - - 0 1",
                mirrored);

        int origin = Move.squareStringToIndex(
                mirrored ? "d5" : "d4");

        Set<String> actual = new HashSet<>();

        for (EngineMove result : search(state, 100, 1)) {
            if (result.move().getFrom() == origin) {
                actual.add(result.move().getUciMoveString());
            }
        }

        assertEquals(
                Set.of(mirrored ? mirrorUci("d4e5") : "d4e5"),
                actual);
    }

    private static void assertMate(
            boolean mirrored,
            int depth,
            String fen) {

        GameState state = fixture(fen, mirrored);
        boolean attackerIsWhite = state.isWhiteTurn();

        // make sure the fixture really requires the stated number of moves
        if (depth > 1) {
            assertFalse(
                    "Fixture has a shorter forced mate",
                    canForceMate(state, attackerIsWhite, depth - 2));
        }

        Move chosen = bestMove(state, depth);
        GameState nextState = MoveHandler.applyMove(state, chosen);

        // any move proving mate is acceptable, regardless of task order
        assertTrue(
                "Chosen move doesn't force mate: "
                        + chosen.getUciMoveString(),
                canForceMate(nextState, attackerIsWhite, depth - 1));
    }

    private static void assertMultipleMates(boolean mirrored) {
        GameState state = fixture(
                "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1",
                mirrored);

        Set<Move> mates = immediateMatingMoves(state);

        assertTrue("Fixture must have multiple mating moves",
                mates.size() > 1);

        assertTrue(
                "Expected any immediate checkmate",
                mates.contains(bestMove(state, 1)));
    }

    private static void assertTerminal(
            boolean mirrored,
            boolean check,
            String fen) {

        GameState state = fixture(fen, mirrored);

        assertEquals(check, MoveValidator.isInCheck(state));
        assertEquals(0, search(state, 5, 1).length);
    }

    private static void assertPromotion(boolean mirrored) {
        GameState state = fixture(
                "8/P6k/8/8/8/8/8/K7 w - - 0 1",
                mirrored);

        Move chosen = bestMove(state, 1);

        assertEquals(
                mirrored ? "a2a1" : "a7a8",
                chosen.getUciMoveString().substring(0, 4));

        // don't bind this test to a particular promotion heuristic
        assertTrue(chosen.getPromotionPieceClass().isPresent());
    }

    private static void assertCapturePromotion(boolean mirrored) {
        GameState state = fixture(
                "1r6/P6k/8/8/8/8/8/K7 w - - 0 1",
                mirrored);

        Move chosen = bestMove(state, 1);

        assertEquals(
                mirrored ? "a2b1" : "a7b8",
                chosen.getUciMoveString().substring(0, 4));

        assertTrue(chosen.getPromotionPieceClass().isPresent());
    }

    private static void assertEnPassant(boolean mirrored) {
        GameState state = fixture(
                "7k/8/8/3pP3/8/8/8/7K w - d6 0 1",
                mirrored);

        assertBestMoveAmong(
                state,
                1,
                mirrored ? "e4d3" : "e5d6");
    }

    private static void assertLegalDistinct(
            GameState state,
            EngineMove[] results) {

        Set<Move> distinct = new HashSet<>();

        for (EngineMove result : results) {
            assertNotNull(result);
            assertNotNull(result.move());
            assertTrue(MoveValidator.isLegalMove(state, result.move()));

            assertTrue(
                    "Duplicate move: " + result.move().getUciMoveString(),
                    distinct.add(result.move()));
        }
    }

    private static Set<String> uciMoves(EngineMove[] results) {
        Set<String> moves = new HashSet<>();

        for (EngineMove result : results) {
            moves.add(result.move().getUciMoveString());
        }

        return moves;
    }

    private static void assertSearchPreservesState(String fen) {
        GameState state = position(fen);
        List<Object> before = snapshot(state);

        EngineMove[] results = search(state, 100, 1);

        assertTrue(results.length > 0);
        assertLegalDistinct(state, results);
        assertEquals(before, snapshot(state));
    }

    /**
     * Record board contents and every state field.
     * No dependency on GameState.equals or FEN encoding.
     */
    private static List<Object> snapshot(GameState state) {
        List<Object> values = new ArrayList<>();

        for (int idx = 0; idx < 64; idx++) {
            Piece piece = state.getPieceAtIndex(idx);

            values.add(piece == null ? null : piece.getClass());
            values.add(piece == null ? null : piece.isWhite());
        }

        values.add(state.isWhiteTurn());
        values.add(state.isWhiteKingsideCastlingAllowed());
        values.add(state.isWhiteQueensideCastlingAllowed());
        values.add(state.isBlackKingsideCastlingAllowed());
        values.add(state.isBlackQueensideCastlingAllowed());
        values.add(state.getEnPassantSquare());
        values.add(state.getHalfmoveClock());
        values.add(state.getFullmoveNumber());

        return values;
    }

    private static Set<Move> immediateMatingMoves(GameState state) {
        Set<Move> mates = new HashSet<>();

        for (Move move : MoveValidator.getLegalMoves(state)) {
            GameState nextState = MoveHandler.applyMove(state, move);

            if (MoveValidator.getLegalMoves(nextState).isEmpty()
                    && MoveValidator.isInCheck(nextState)) {
                mates.add(move);
            }
        }

        return mates;
    }

    /**
     * Verify forced mate independently of engine evaluation scores.
     * One attacking continuation must win against every defense.
     */
    private static boolean canForceMate(
            GameState state,
            boolean attackerIsWhite,
            int remainingDepth) {

        Set<Move> moves = MoveValidator.getLegalMoves(state);

        if (moves.isEmpty()) {
            return state.isWhiteTurn() != attackerIsWhite
                    && MoveValidator.isInCheck(state);
        }

        if (remainingDepth == 0) {
            return false;
        }

        boolean attackingTurn =
                state.isWhiteTurn() == attackerIsWhite;

        for (Move move : moves) {
            boolean winning = canForceMate(
                    MoveHandler.applyMove(state, move),
                    attackerIsWhite,
                    remainingDepth - 1);

            if (attackingTurn && winning) {
                return true;
            }

            if (!attackingTurn && !winning) {
                return false;
            }
        }

        return !attackingTurn;
    }

    /**
     * Reflect ranks and swap colors to reuse a tactical fixture for black.
     * Files stay unchanged: a1 becomes a8, e5 becomes e4.
     */
    private static GameState fixture(String fen, boolean mirrored) {
        return position(mirrored ? mirrorFen(fen) : fen);
    }

    private static String mirrorFen(String fen) {
        String[] fields = fen.split(" ");
        String[] ranks = fields[0].split("/");
        StringBuilder board = new StringBuilder();

        for (int rank = 7; rank >= 0; rank--) {
            if (board.length() > 0) {
                board.append('/');
            }

            for (char symbol : ranks[rank].toCharArray()) {
                if (Character.isUpperCase(symbol)) {
                    board.append(Character.toLowerCase(symbol));
                } else if (Character.isLowerCase(symbol)) {
                    board.append(Character.toUpperCase(symbol));
                } else {
                    board.append(symbol);
                }
            }
        }

        String rights = fields[2];
        StringBuilder mirroredRights = new StringBuilder();

        // preserve canonical KQkq ordering after swapping colors
        if (rights.contains("k")) mirroredRights.append('K');
        if (rights.contains("q")) mirroredRights.append('Q');
        if (rights.contains("K")) mirroredRights.append('k');
        if (rights.contains("Q")) mirroredRights.append('q');

        String enPassant = fields[3].equals("-")
                ? "-"
                : mirrorSquare(fields[3]);

        return board
                + " " + (fields[1].equals("w") ? "b" : "w")
                + " " + (mirroredRights.length() == 0
                ? "-" : mirroredRights.toString())
                + " " + enPassant
                + " " + fields[4]
                + " " + fields[5];
    }

    private static String mirrorSquare(String square) {
        return "" + square.charAt(0)
                + (char) ('1' + '8' - square.charAt(1));
    }

    private static String mirrorUci(String uci) {
        return mirrorSquare(uci.substring(0, 2))
                + mirrorSquare(uci.substring(2, 4))
                + uci.substring(4);
    }
}