package com.github.lfbr0.board;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.github.lfbr0.board.piece.BishopPiece;
import com.github.lfbr0.board.piece.KingPiece;
import com.github.lfbr0.board.piece.KnightPiece;
import com.github.lfbr0.board.piece.PawnPiece;
import com.github.lfbr0.board.piece.QueenPiece;
import com.github.lfbr0.board.piece.RookPiece;
import org.junit.Test;

/**
 * Tests for the FEN encoding and decoding logic.
 * AI generated because I really don't like writing tests.
 */
public class FenCodecTests {

    private static final String STARTING_PLACEMENT =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";

    @Test
    public void decodeDefaultFenCreatesStartingPosition() {
        Piece[] board = FenCodec.decode(FenCodec.DEFAULT_FEN_STRING).getBoardCopy();

        assertEquals(64, board.length);

        Class<?>[] backRank = {
                RookPiece.class,
                KnightPiece.class,
                BishopPiece.class,
                QueenPiece.class,
                KingPiece.class,
                BishopPiece.class,
                KnightPiece.class,
                RookPiece.class
        };

        for (int file = 0; file < 8; file++) {
            String fileName = String.valueOf((char) ('a' + file));

            assertPiece(board, fileName + "1", backRank[file], true);
            assertPiece(board, fileName + "2", PawnPiece.class, true);
            assertPiece(board, fileName + "7", PawnPiece.class, false);
            assertPiece(board, fileName + "8", backRank[file], false);

            for (int rank = 3; rank <= 6; rank++) {
                assertEmpty(board, fileName + rank);
            }
        }
    }

    @Test
    public void decodeEmptyBoardReturnsAllNullSquares() {
        Piece[] board = FenCodec.decode("8/8/8/8/8/8/8/8").getBoardCopy();

        assertEquals(64, board.length);
        assertPieceCount(board, 0);
    }

    @Test
    public void decodeSingleWhiteKingOnH1() {
        Piece[] board = FenCodec.decode("8/8/8/8/8/8/8/7K").getBoardCopy();

        assertPiece(board, "h1", KingPiece.class, true);
        assertPieceCount(board, 1);
    }

    @Test
    public void decodeSingleBlackQueenOnD8() {
        Piece[] board = FenCodec.decode("3q4/8/8/8/8/8/8/8").getBoardCopy();

        assertPiece(board, "d8", QueenPiece.class, false);
        assertPieceCount(board, 1);
    }

    @Test
    public void decodeHandlesLeadingAndTrailingEmptySquares() {
        Piece[] board = FenCodec.decode("2k5/7K/8/8/8/8/8/8").getBoardCopy();

        assertPiece(board, "c8", KingPiece.class, false);
        assertPiece(board, "h7", KingPiece.class, true);
        assertEmpty(board, "a8");
        assertEmpty(board, "h8");
        assertEmpty(board, "a7");
        assertPieceCount(board, 2);
    }

    @Test
    public void decodeReadsLowercaseAndUppercaseSymbols() {
        Piece[] board = FenCodec.decode("8/8/8/8/3N4/8/8/3n4").getBoardCopy();

        assertPiece(board, "d4", KnightPiece.class, true);
        assertPiece(board, "d1", KnightPiece.class, false);
        assertPieceCount(board, 2);
    }

    @Test
    public void decodeReadsEveryPieceTypeInBothColors() {
        Piece[] board = FenCodec.decode(
                "prnbqk2/8/8/8/8/8/8/PRNBQK2").getBoardCopy();

        Class<?>[] types = {
                PawnPiece.class,
                RookPiece.class,
                KnightPiece.class,
                BishopPiece.class,
                QueenPiece.class,
                KingPiece.class
        };

        for (int file = 0; file < types.length; file++) {
            String fileName = String.valueOf((char) ('a' + file));
            assertPiece(board, fileName + "8", types[file], false);
            assertPiece(board, fileName + "1", types[file], true);
        }

        assertPieceCount(board, 12);
    }

    @Test
    public void decodeMapsAllFourCornersCorrectly() {
        Piece[] board = FenCodec.decode(
                "r6q/8/8/8/8/8/8/R6Q").getBoardCopy();

        assertPiece(board, "a8", RookPiece.class, false);
        assertPiece(board, "h8", QueenPiece.class, false);
        assertPiece(board, "a1", RookPiece.class, true);
        assertPiece(board, "h1", QueenPiece.class, true);
        assertPieceCount(board, 4);
    }

    @Test
    public void decodeHandlesRanksWithoutEmptySquares() {
        Piece[] board = FenCodec.decode(
                "rnbqkbnr/8/8/8/8/8/8/RNBQKBNR").getBoardCopy();

        assertPiece(board, "a8", RookPiece.class, false);
        assertPiece(board, "h8", RookPiece.class, false);
        assertPiece(board, "a1", RookPiece.class, true);
        assertPiece(board, "h1", RookPiece.class, true);
        assertPieceCount(board, 16);
    }

    @Test
    public void decodeReadsBoardWithFullMetadata() {
        Piece[] board = FenCodec.decode(
                "8/8/8/8/3p4/8/8/4K3 w - - 0 1").getBoardCopy();

        assertPiece(board, "d4", PawnPiece.class, false);
        assertPiece(board, "e1", KingPiece.class, true);
        assertPieceCount(board, 2);
    }

    @Test
    public void decodeReadsBoardWithCastlingAndEnPassantMetadata() {
        Piece[] board = FenCodec.decode(
                "r3k2r/8/8/8/4P3/8/8/R3K2R b KQkq e3 0 1").getBoardCopy();

        assertPiece(board, "a1", RookPiece.class, true);
        assertPiece(board, "h1", RookPiece.class, true);
        assertPiece(board, "e1", KingPiece.class, true);
        assertPiece(board, "a8", RookPiece.class, false);
        assertPiece(board, "h8", RookPiece.class, false);
        assertPiece(board, "e8", KingPiece.class, false);
        assertPiece(board, "e4", PawnPiece.class, true);
        assertPieceCount(board, 7);
    }

    @Test
    public void decodeBoardFromSpecificPositionWithMultiplePieces() {
        Piece[] board = FenCodec.decode(
                "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R"
                        + " w KQkq - 2 2").getBoardCopy();

        assertPiece(board, "a8", RookPiece.class, false);
        assertPiece(board, "c8", BishopPiece.class, false);
        assertPiece(board, "c6", KnightPiece.class, false);
        assertPiece(board, "f3", KnightPiece.class, true);
        assertPiece(board, "e5", PawnPiece.class, false);
        assertPiece(board, "e4", PawnPiece.class, true);
        assertPiece(board, "e8", KingPiece.class, false);

        assertEmpty(board, "b8");
        assertEmpty(board, "g1");
        assertEmpty(board, "e2");
        assertEmpty(board, "e7");
        assertPieceCount(board, 32);
    }

    @Test
    public void decodeRejectsInvalidPieceSymbols() {
        assertInvalidPlacement(
                "x7/8/8/8/8/8/8/8",
                "X7/8/8/8/8/8/8/8",
                "8/8/8/8/8/8/8/7?",
                "8/8/8/8/8/8/8/8x");
    }

    @Test
    public void decodeRejectsDigitsOutsideOneToEight() {
        assertInvalidPlacement(
                "0/8/8/8/8/8/8/8",
                "9/8/8/8/8/8/8/8",
                "10/8/8/8/8/8/8/8",
                "08/8/8/8/8/8/8/8",
                "80/8/8/8/8/8/8/8");
    }

    @Test
    public void decodeRejectsNonAsciiDigits() {
        assertInvalidPlacement(
                "\u0668/8/8/8/8/8/8/8",
                "\uFF18/8/8/8/8/8/8/8");
    }

    @Test
    public void decodeRejectsWrongNumberOfRanks() {
        assertInvalidPlacement(
                "8/8/8/8/8/8/8",
                "8/8/8/8/8/8/8/8/8");
    }

    @Test
    public void decodeRejectsEmptyRanksAndExtraSeparators() {
        assertInvalidPlacement(
                "/8/8/8/8/8/8/8/8",
                "8/8/8/8/8/8/8/8/",
                "8//8/8/8/8/8/8");
    }

    @Test
    public void decodeRejectsUnderfilledRanksAtEveryPosition() {
        for (int rank = 0; rank < 8; rank++) {
            assertInvalidPlacement(placementWithRank(rank, "7"));
        }
    }

    @Test
    public void decodeRejectsOverflowingRanksAtEveryPosition() {
        for (int rank = 0; rank < 8; rank++) {
            assertInvalidPlacement(
                    placementWithRank(rank, "8K"),
                    placementWithRank(rank, "K8"),
                    placementWithRank(rank, "QQQQQQQQQ"));
        }
    }

    @Test
    public void decodeRejectsConsecutiveDigitsEvenWhenTheySumToEight() {
        assertInvalidPlacement(
                "44/8/8/8/8/8/8/8",
                "17/8/8/8/8/8/8/8",
                "11111111/8/8/8/8/8/8/8",
                "K34/8/8/8/8/8/8/8");
    }

    @Test
    public void decodeRejectsBlankFen() {
        assertInvalidPlacement("", "   ", "\t", "\n");
    }

    @Test
    public void decodeRejectsNullFen() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FenCodec.decode(null));
    }

    @Test
    public void encodeStartingPositionMatchesFullFen() {
        GameState state = FenCodec.decode(FenCodec.DEFAULT_FEN_STRING);

        assertEquals(
                STARTING_PLACEMENT + " w KQkq - 0 1",
                FenCodec.encode(state));
    }

    @Test
    public void encodeEmptyBoardProducesAllEmptyRanks() {
        assertEquals(
                "8/8/8/8/8/8/8/8 w - - 0 1",
                FenCodec.encode(new GameState(new Piece[64], true)));
    }

    @Test
    public void encodeSinglePieceBoard() {
        Piece[] board = new Piece[64];
        board[squareToIndex("h1")] = new KingPiece(true);

        assertEquals("8/8/8/8/8/8/8/7K w - - 0 1", FenCodec.encode(new GameState(board, true)));
    }

    @Test
    public void encodeMapsAllFourCornersCorrectly() {
        Piece[] board = new Piece[64];
        board[squareToIndex("a8")] = new RookPiece(false);
        board[squareToIndex("h8")] = new QueenPiece(false);
        board[squareToIndex("a1")] = new RookPiece(true);
        board[squareToIndex("h1")] = new QueenPiece(true);

        assertEquals("r6q/8/8/8/8/8/8/R6Q w - - 0 1", FenCodec.encode(new GameState(board, true)));
    }

    @Test
    public void encodeEveryPieceTypeInBothColors() {
        Piece[] board = new Piece[64];

        board[squareToIndex("a8")] = new PawnPiece(false);
        board[squareToIndex("b8")] = new RookPiece(false);
        board[squareToIndex("c8")] = new KnightPiece(false);
        board[squareToIndex("d8")] = new BishopPiece(false);
        board[squareToIndex("e8")] = new QueenPiece(false);
        board[squareToIndex("f8")] = new KingPiece(false);

        board[squareToIndex("a1")] = new PawnPiece(true);
        board[squareToIndex("b1")] = new RookPiece(true);
        board[squareToIndex("c1")] = new KnightPiece(true);
        board[squareToIndex("d1")] = new BishopPiece(true);
        board[squareToIndex("e1")] = new QueenPiece(true);
        board[squareToIndex("f1")] = new KingPiece(true);

        assertEquals(
                "prnbqk2/8/8/8/8/8/8/PRNBQK2 w - - 0 1",
                FenCodec.encode(new GameState(board, true)));
    }

    @Test
    public void encodeMultiplePiecesPreservesBoardOrder() {
        Piece[] board = new Piece[64];
        board[squareToIndex("a1")] = new RookPiece(true);
        board[squareToIndex("e1")] = new KingPiece(true);
        board[squareToIndex("d8")] = new QueenPiece(false);
        board[squareToIndex("c7")] = new BishopPiece(false);

        assertEquals(
                "3q4/2b5/8/8/8/8/8/R3K3 w - - 0 1",
                FenCodec.encode(new GameState(board, true)));
    }

    @Test
    public void encodeCompressesSeparateEmptyRunsWithinRank() {
        Piece[] board = new Piece[64];
        board[squareToIndex("b4")] = new KnightPiece(true);
        board[squareToIndex("e4")] = new BishopPiece(false);
        board[squareToIndex("g4")] = new RookPiece(true);

        assertEquals(
                "8/8/8/8/1N2b1R1/8/8/8 w - - 0 1",
                FenCodec.encode(new GameState(board, true)));
    }

    @Test
    public void encodeDoesNotInsertDigitsBetweenAdjacentPieces() {
        Piece[] board = new Piece[64];
        board[squareToIndex("d4")] = new KingPiece(true);
        board[squareToIndex("e4")] = new QueenPiece(false);

        assertEquals(
                "8/8/8/8/3Kq3/8/8/8 w - - 0 1",
                FenCodec.encode(new GameState(board, true)));
    }

    @Test
    public void encodeHandlesCompletelyOccupiedRank() {
        Piece[] board = new Piece[64];

        for (int file = 0; file < 8; file++) {
            board[squareToIndex("a4") + file] = new PawnPiece(true);
        }

        assertEquals(
                "8/8/8/8/PPPPPPPP/8/8/8 w - - 0 1",
                FenCodec.encode(new GameState(board, true)));
    }

    @Test
    public void encodeRoundTripsThroughDecode() {
        Piece[] original = new Piece[64];
        original[squareToIndex("a1")] = new RookPiece(true);
        original[squareToIndex("e1")] = new KingPiece(true);
        original[squareToIndex("d8")] = new QueenPiece(false);
        original[squareToIndex("h7")] = new PawnPiece(true);
        original[squareToIndex("g2")] = new KnightPiece(false);

        Piece[] decoded = FenCodec.decode(FenCodec.encode(new GameState(original, true))).getBoardCopy();

        assertBoardsEquivalent(original, decoded);
    }

    @Test
    public void decodeRoundTripsThroughEncodeForCanonicalPlacements() {
        String[] placements = {
                STARTING_PLACEMENT + " w - - 0 1",
                "8/8/8/8/8/8/8/8 w - - 0 1",
                "7k/8/8/8/8/8/8/K7 w - - 0 1",
                "r6q/8/8/8/8/8/8/R6Q w - - 0 1",
                "8/8/8/8/1N2b1R1/8/8/8 w - - 0 1",
                "prnbqk2/8/8/8/8/8/8/PRNBQK2 w - - 0 1"
        };

        for (String placement : placements) {
            assertEquals(
                    "Round trip for " + placement,
                    placement,
                    FenCodec.encode(FenCodec.decode(placement)));
        }
    }

    @Test
    public void encodeDoesNotModifyBoardContents() {
        Piece[] board = new Piece[64];
        board[squareToIndex("a1")] = new RookPiece(true);
        board[squareToIndex("d8")] = new QueenPiece(false);

        Piece[] before = board.clone();

        FenCodec.encode(new GameState(board, true));

        for (int i = 0; i < board.length; i++) {
            org.junit.Assert.assertSame(
                    "Piece reference at index " + i,
                    before[i],
                    board[i]);
        }
    }

    @Test
    public void encodeRejectsBoardsThatAreNotSixtyFourSquaresLong() {
        int[] invalidLengths = {0, 1, 63, 65, 128};

        for (int length : invalidLengths) {
            assertThrows(
                    "Board length " + length,
                    IllegalArgumentException.class,
                    () -> FenCodec.encode(new GameState(new Piece[length], true)));
        }
    }

    @Test
    public void encodeRejectsNullState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FenCodec.encode(null));
    }

    @Test
    public void encodeIncludesWhiteTurn() {
        GameState state = new GameState(new Piece[64], true);

        assertEquals("8/8/8/8/8/8/8/8 w - - 0 1", FenCodec.encode(state));
    }

    @Test
    public void encodeIncludesBlackTurn() {
        GameState state = new GameState(new Piece[64], false);

        assertEquals("8/8/8/8/8/8/8/8 b - - 0 1", FenCodec.encode(state));
    }

    @Test
    public void encodeRoundTripPreservesBoardAndTurnForBothColors() {
        Piece[] board = new Piece[64];
        board[squareToIndex("e1")] = new KingPiece(true);
        board[squareToIndex("e8")] = new KingPiece(false);

        for (boolean whiteTurn : new boolean[] {true, false}) {
            GameState original = new GameState(board, whiteTurn);
            GameState decoded = FenCodec.decode(FenCodec.encode(original));

            assertBoardsEquivalent(original.getBoardCopy(), decoded.getBoardCopy());
            assertEquals("Active color", whiteTurn, decoded.isWhiteTurn());
        }
    }

    @Test
    public void decodeReadsWhiteTurn() {
        GameState state = FenCodec.decode(
                STARTING_PLACEMENT + " w KQkq - 0 1");

        assertNotNull(state);
        assertEquals(true, state.isWhiteTurn());
        assertEquals(64, state.getBoardCopy().length);
    }

    @Test
    public void decodeReadsBlackTurn() {
        GameState state = FenCodec.decode(
                STARTING_PLACEMENT + " b KQkq - 0 1");

        assertNotNull(state);
        assertFalse(state.isWhiteTurn());
        assertPiece(state.getBoardCopy(), "e1", KingPiece.class, true);
        assertPiece(state.getBoardCopy(), "e8", KingPiece.class, false);
    }

    @Test
    public void decodePlacementOnlyDefaultsToWhiteTurn() {
        GameState state = FenCodec.decode(STARTING_PLACEMENT);

        assertEquals(true, state.isWhiteTurn());
    }

    @Test
    public void decodeRejectsInvalidActiveColor() {
        for (String color : new String[] {"x", "W", "B", "white", "black", "-"}) {
            assertThrows(
                    "Invalid active color: " + color,
                    IllegalArgumentException.class,
                    () -> FenCodec.decode(STARTING_PLACEMENT + " " + color));
        }
    }

    @Test
    public void decodeAbbreviatedInputsUseDefaultMetadata() {
        for (String input : new String[] {
                STARTING_PLACEMENT, STARTING_PLACEMENT + " w",
                STARTING_PLACEMENT + " b"
        }) {
            GameState state = FenCodec.decode(input);

            assertEquals(!input.endsWith(" b"), state.isWhiteTurn());
            assertCastling(state, false, false, false, false);
            assertNull(state.getEnPassantSquare());
            assertEquals(0, state.getHalfmoveClock());
            assertEquals(1, state.getFullmoveNumber());
        }
    }

    @Test
    public void decodeReadsEveryCastlingCombination() {
        for (int mask = 0; mask < 16; mask++) {
            boolean wk = (mask & 1) != 0;
            boolean wq = (mask & 2) != 0;
            boolean bk = (mask & 4) != 0;
            boolean bq = (mask & 8) != 0;
            String rights = castlingToken(wk, wq, bk, bq);

            GameState state = FenCodec.decode(
                    STARTING_PLACEMENT + " w " + rights + " - 0 1");

            assertCastling(state, wk, wq, bk, bq);
        }
    }

    @Test
    public void encodeWritesEveryCastlingCombinationInCanonicalOrder() {
        for (int mask = 0; mask < 16; mask++) {
            boolean wk = (mask & 1) != 0;
            boolean wq = (mask & 2) != 0;
            boolean bk = (mask & 4) != 0;
            boolean bq = (mask & 8) != 0;
            GameState state = new GameState(
                    new Piece[64], true, wk, wq, bk, bq, null, 0, 1);

            assertEquals(
                    "8/8/8/8/8/8/8/8 w "
                            + castlingToken(wk, wq, bk, bq) + " - 0 1",
                    FenCodec.encode(state));
        }
    }

    @Test
    public void decodeReadsEnPassantTargetsOnBothRanks() {
        for (char file = 'a'; file <= 'h'; file++) {
            for (String rank : new String[] {"3", "6"}) {
                String target = file + rank;
                String color = rank.equals("3") ? "b" : "w";
                GameState state = FenCodec.decode(
                        STARTING_PLACEMENT + " " + color + " - " + target + " 0 12");

                assertEquals(target, state.getEnPassantSquare());
            }
        }
    }

    @Test
    public void decodeAbsentEnPassantTargetBecomesNull() {
        GameState state = FenCodec.decode(
                STARTING_PLACEMENT + " w - - 0 1");

        assertNull(state.getEnPassantSquare());
    }

    @Test
    public void decodeReadsMoveCounters() {
        GameState state = FenCodec.decode(
                STARTING_PLACEMENT + " b - - 99 123");

        assertEquals(99, state.getHalfmoveClock());
        assertEquals(123, state.getFullmoveNumber());
    }

    @Test
    public void decodeAcceptsMaximumIntegerCounters() {
        GameState state = FenCodec.decode(
                STARTING_PLACEMENT + " w - - 2147483647 2147483647");

        assertEquals(Integer.MAX_VALUE, state.getHalfmoveClock());
        assertEquals(Integer.MAX_VALUE, state.getFullmoveNumber());
    }

    @Test
    public void encodeWritesAllMetadataFromAnIndependentState() {
        Piece[] board = new Piece[64];
        board[squareToIndex("e1")] = new KingPiece(true);
        board[squareToIndex("e8")] = new KingPiece(false);
        GameState state = new GameState(
                board, false, true, false, false, true, "e3", 0, 42);

        assertEquals(
                "4k3/8/8/8/8/8/8/4K3 b Kq e3 0 42",
                FenCodec.encode(state));
    }

    @Test
    public void fullFenRoundTripPreservesEveryField() {
        String[] inputs = {
                STARTING_PLACEMENT + " w KQkq - 0 1",
                "r3k2r/8/8/8/4P3/8/8/R3K2R b Kq e3 0 12",
                "r3k2r/8/8/3p4/8/8/8/R3K2R w Qk d6 0 23",
                "4k3/8/8/8/8/8/8/4K3 b - - 87 123"
        };

        for (String input : inputs) {
            assertEquals(input, FenCodec.encode(FenCodec.decode(input)));
        }
    }

    @Test
    public void stateRoundTripPreservesEveryField() {
        Piece[] board = new Piece[64];
        board[squareToIndex("e1")] = new KingPiece(true);
        board[squareToIndex("e8")] = new KingPiece(false);

        for (boolean white : new boolean[] {true, false}) {
            GameState original = new GameState(
                    board, white, false, true, true, false,
                    white ? "d6" : "e3", 0, 37);
            GameState decoded = FenCodec.decode(FenCodec.encode(original));

            assertBoardsEquivalent(original.getBoardCopy(), decoded.getBoardCopy());
            assertEquals(original.isWhiteTurn(), decoded.isWhiteTurn());
            assertCastling(decoded, false, true, true, false);
            assertEquals(original.getEnPassantSquare(), decoded.getEnPassantSquare());
            assertEquals(original.getHalfmoveClock(), decoded.getHalfmoveClock());
            assertEquals(original.getFullmoveNumber(), decoded.getFullmoveNumber());
        }
    }

    @Test
    public void decodeAcceptsSurroundingAndRepeatedWhitespace() {
        GameState state = FenCodec.decode(
                "  " + STARTING_PLACEMENT + "  b\tKq  -  7  42  ");

        assertEquals(
                STARTING_PLACEMENT + " b Kq - 7 42",
                FenCodec.encode(state));
    }

    @Test
    public void decodeRejectsIncompleteOrExtraFields() {
        assertInvalidPlacement(
                STARTING_PLACEMENT + " w KQkq",
                STARTING_PLACEMENT + " w KQkq -",
                STARTING_PLACEMENT + " w KQkq - 0",
                STARTING_PLACEMENT + " w KQkq - 0 1 extra");
    }

    @Test
    public void decodeRejectsMalformedCastlingRights() {
        for (String rights : new String[] {
                "KK", "QQ", "kk", "qq", "qK", "QK", "kK",
                "KQkqK", "K-", "--", "A", "|"
        }) {
            assertInvalidPlacement(STARTING_PLACEMENT + " w " + rights + " - 0 1");
        }
    }

    @Test
    public void decodeRejectsMalformedEnPassantTargets() {
        for (String target : new String[] {
                "e1", "e2", "e4", "e5", "e7", "e8",
                "i3", "a9", "E3", "e33", "null", "--"
        }) {
            assertInvalidPlacement(STARTING_PLACEMENT + " w - " + target + " 0 1");
        }
    }

    @Test
    public void decodeRejectsInvalidMoveCounters() {
        for (String counter : new String[] {
                "-1", "+1", "1.5", "x", "2147483648", "99999999999999999999"
        }) {
            assertInvalidPlacement(
                    STARTING_PLACEMENT + " w - - " + counter + " 1",
                    STARTING_PLACEMENT + " w - - 0 " + counter);
        }

        assertInvalidPlacement(STARTING_PLACEMENT + " w - - 0 0");
    }

    @Test
    public void encodeRejectsNullBoard() {
        GameState state = new GameState(null, true);

        assertThrows(IllegalArgumentException.class, () -> FenCodec.encode(state));
    }

    @Test
    public void encodeRejectsInvalidEnPassantTargets() {
        for (String target : new String[] {"-", "e4", "E3", "i6", ""}) {
            GameState state = new GameState(
                    new Piece[64], true,
                    false, false, false, false, target, 0, 1);

            assertThrows(
                    "Invalid target " + target,
                    IllegalArgumentException.class,
                    () -> FenCodec.encode(state));
        }
    }

    @Test
    public void encodeRejectsNegativeHalfmoveClock() {
        GameState state = new GameState(
                new Piece[64], true,
                false, false, false, false, null, -1, 1);

        assertThrows(IllegalArgumentException.class, () -> FenCodec.encode(state));
    }

    @Test
    public void encodeRejectsNonPositiveFullmoveNumber() {
        for (int number : new int[] {0, -1}) {
            GameState state = new GameState(
                    new Piece[64], true,
                    false, false, false, false, null, 0, number);

            assertThrows(IllegalArgumentException.class, () -> FenCodec.encode(state));
        }
    }

    private static void assertCastling(
            GameState state, boolean wk, boolean wq, boolean bk, boolean bq) {
        assertEquals("White kingside", wk, state.isWhiteKingsideCastlingAllowed());
        assertEquals("White queenside", wq, state.isWhiteQueensideCastlingAllowed());
        assertEquals("Black kingside", bk, state.isBlackKingsideCastlingAllowed());
        assertEquals("Black queenside", bq, state.isBlackQueensideCastlingAllowed());
    }

    private static String castlingToken(boolean wk, boolean wq, boolean bk, boolean bq) {
        String rights = (wk ? "K" : "") + (wq ? "Q" : "")
                + (bk ? "k" : "") + (bq ? "q" : "");
        return rights.isEmpty() ? "-" : rights;
    }

    private static void assertPiece(
            Piece[] board,
            String square,
            Class<?> expectedType,
            boolean white) {

        Piece piece = board[squareToIndex(square)];

        assertNotNull("Expected a piece on " + square, piece);
        assertEquals(
                "Piece type on " + square,
                expectedType,
                piece.getClass());
        assertEquals(
                "Piece color on " + square,
                white,
                piece.isWhite());
    }

    private static void assertEmpty(Piece[] board, String square) {
        assertNull(
                "Expected an empty square on " + square,
                board[squareToIndex(square)]);
    }

    private static void assertPieceCount(
            Piece[] board,
            int expectedCount) {

        int actualCount = 0;

        for (Piece piece : board) {
            if (piece != null) {
                actualCount++;
            }
        }

        assertEquals("Number of pieces", expectedCount, actualCount);
    }

    private static void assertBoardsEquivalent(
            Piece[] expected,
            Piece[] actual) {

        assertNotNull("Decoded board", actual);
        assertEquals("Board length", expected.length, actual.length);

        for (int i = 0; i < expected.length; i++) {
            String location = " at index " + i;

            if (expected[i] == null) {
                assertNull("Expected empty square" + location, actual[i]);
            } else {
                assertNotNull("Expected piece" + location, actual[i]);
                assertEquals(
                        "Piece type" + location,
                        expected[i].getClass(),
                        actual[i].getClass());
                assertEquals(
                        "Piece color" + location,
                        expected[i].isWhite(),
                        actual[i].isWhite());
            }
        }
    }

    private static void assertInvalidPlacement(String... placements) {
        for (String placement : placements) {
            assertThrows(
                    "Expected rejection of [" + placement + "]",
                    IllegalArgumentException.class,
                    () -> FenCodec.decode(placement));
        }
    }

    private static String placementWithRank(int rankIndex, String rank) {
        String[] ranks = {"8", "8", "8", "8", "8", "8", "8", "8"};
        ranks[rankIndex] = rank;
        return String.join("/", ranks);
    }

    private static int squareToIndex(String square) {
        int file = square.charAt(0) - 'a';
        int rank = square.charAt(1) - '1';
        return rank * 8 + file;
    }
}