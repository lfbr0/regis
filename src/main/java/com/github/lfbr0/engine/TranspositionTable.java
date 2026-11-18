package com.github.lfbr0.engine;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/4/26
 * @project regis
 */
public class TranspositionTable {

    private static final long EXPECTED_POLYGLOT_HASH = 0x463B96181691FC9CL;

    /**
     * For record transposition table entries
     */
    public record TranspositionTableEntry(long hash, double score, int depth, int flag) {
        public static final int EXACT = 0;
        public static final int LOWER_BOUND = 1; // Beta cutoff (score >= beta)
        public static final int UPPER_BOUND = 2; // Alpha cutoff (score <= alpha)
    }

    // Polyglot orders colors as black, then white, for each piece type
    private static final int BLACK = 0;
    private static final int WHITE = 1;
    private static final int PAWN = 0;
    private static final int KNIGHT = 1;
    private static final int BISHOP = 2;
    private static final int ROOK = 3;
    private static final int QUEEN = 4;
    private static final int KING = 5;

    // Record score for each position
    private final Map<Long, TranspositionTableEntry> boardToScoreTable;

    // fixed keys shared by Polyglot opening books and our position hashing
    // 0..767: pieces, 768..771: castling, 772..779: en passant, 780: white to move
    private final long[] polyglotKeys;

    public TranspositionTable() {
        // verify the loaded keys produce the expected Polyglot starting hash
        polyglotKeys = loadPolyglotKeys();
        if (hash(new GameState()) != EXPECTED_POLYGLOT_HASH) {
            throw new IllegalStateException("Invalid Polyglot keys or hashing implementation");
        }

        // concurrent map
        boardToScoreTable = new ConcurrentHashMap<>();
    }

    /**
     * Loads the fixed Polyglot keys from the bundled resource.
     * @return 781 Polyglot keys
     */
    private long[] loadPolyglotKeys() {
        try (InputStream resource = TranspositionTable.class.getResourceAsStream("/polyglot-keys.bin")) {
            if (resource == null) {
                throw new IllegalStateException("Missing polyglot-keys.bin resource");
            }

            // readLong reads big-endian values, matching our resource format
            DataInputStream input = new DataInputStream(resource);
            long[] keys = new long[781];

            for (int i = 0; i < keys.length; i++) {
                keys[i] = input.readLong();
            }

            // the resource must contain exactly 781 longs
            if (input.read() != -1) {
                throw new IllegalStateException("Unexpected extra data in polyglot-keys.bin");
            }

            return keys;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Polyglot keys", e);
        }
    }

    public boolean hasEntry(GameState state) {
        return boardToScoreTable.containsKey(hash(state));
    }

    public TranspositionTableEntry getEntry(GameState state) {
        return boardToScoreTable.get(hash(state));
    }

    public void setEntry(GameState state, double score, int depth, int flag) {
        boardToScoreTable.compute(hash(state), (h, entry) -> {
            if (entry == null || entry.depth() <= depth) {
                return new TranspositionTableEntry(h, score, depth, flag);
            }
            return entry;
        });
    }

    /**
     * Computes the Polyglot Zobrist hash for current game state.
     * @param state game state
     * @return hash
     */
    public long hash(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("Chess game state cannot be null");
        }

        long hash = 0L;

        // hash board - our indexing already matches Polyglot: a1 = 0
        for (int idx = 0; idx < 64; idx++) {
            Piece piece = state.getPieceAtIndex(idx);

            if (piece == null) {
                continue;
            }

            hash ^= getPieceKey(piece, idx);
        }

        // hash current side - Polyglot adds this key for white to move
        if (state.isWhiteTurn()) {
            hash ^= polyglotKeys[780];
        }

        // hash castling rights - each allowed right has its own key
        if (state.isWhiteKingsideCastlingAllowed()) {
            hash ^= polyglotKeys[768];
        }
        if (state.isWhiteQueensideCastlingAllowed()) {
            hash ^= polyglotKeys[769];
        }
        if (state.isBlackKingsideCastlingAllowed()) {
            hash ^= polyglotKeys[770];
        }
        if (state.isBlackQueensideCastlingAllowed()) {
            hash ^= polyglotKeys[771];
        }

        // en passant hash
        String enPassantSquare = state.getEnPassantSquare();
        if (enPassantSquare != null
                && !enPassantSquare.isEmpty()
                && !enPassantSquare.equals("-")) {

            if (!enPassantSquare.matches("[a-h][36]")) {
                throw new IllegalArgumentException("Invalid en passant square: " + enPassantSquare);
            }

            int enPassantFile = enPassantSquare.charAt(0) - 'a'; // a=0, h=7
            int enPassantRank = enPassantSquare.charAt(1) - '1'; // rank 1=0

            // find the rank where a capturing pawn would stand
            int pawnRank = enPassantRank + (state.isWhiteTurn() ? -1 : 1);
            int sameFileSquare = pawnRank * 8 + enPassantFile;

            // check adjacent files without wrapping across board edges
            boolean pawnOnLeft = enPassantFile > 0
                    && isFriendlyPawn(state, sameFileSquare - 1);

            boolean pawnOnRight = enPassantFile < 7
                    && isFriendlyPawn(state, sameFileSquare + 1);

            // only hash the file if a friendly pawn could capture there
            // pinned pawns count too - Polyglot doesn't check king safety here
            if (pawnOnLeft || pawnOnRight) {
                hash ^= polyglotKeys[772 + enPassantFile];
            }
        }

        return hash;
    }

    /**
     * Checks if a square contains a pawn of the side to move.
     * @param state game state
     * @param square square to check
     * @return true if a friendly pawn occupies the square
     */
    private boolean isFriendlyPawn(GameState state, int square) {
        Piece piece = state.getPieceAtIndex(square);
        return piece instanceof PawnPiece && piece.isWhite() == state.isWhiteTurn();
    }

    /**
     * Converts piece to its Polyglot key.
     * @param piece piece to hash
     * @param boardIndex square occupied by piece
     * @return piece key
     */
    private long getPieceKey(Piece piece, int boardIndex) {
        int color = piece.isWhite() ? WHITE : BLACK;

        int type;
        if (piece instanceof PawnPiece) {
            type = PAWN;
        } else if (piece instanceof KnightPiece) {
            type = KNIGHT;
        } else if (piece instanceof BishopPiece) {
            type = BISHOP;
        } else if (piece instanceof RookPiece) {
            type = ROOK;
        } else if (piece instanceof QueenPiece) {
            type = QUEEN;
        } else if (piece instanceof KingPiece) {
            type = KING;
        } else {
            throw new IllegalArgumentException("Invalid piece type: " + piece.getClass());
        }

        // black pawn, white pawn, black knight, white knight, and so on
        int pieceIndex = type * 2 + color;

        // each piece/color combination has 64 square keys
        return polyglotKeys[pieceIndex * 64 + boardIndex];
    }
}