package com.github.lfbr0.engine;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.*;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/4/26
 * @project regis
 */
public class TranspositionTable {

    /**
     * For record transposition table entries
     */
    public record TranspositionTableEntry(long hash, double score, int depth, int flag) {
        public static final int EXACT = 0;
        public static final int LOWER_BOUND = 1; // Beta cutoff (score >= beta)
        public static final int UPPER_BOUND = 2; // Alpha cutoff (score <= alpha)
    }

    private static final int WHITE = 0;
    private static final int BLACK = 1;
    private static final int PAWN = 0;
    private static final int KNIGHT = 1;
    private static final int BISHOP = 2;
    private static final int ROOK = 3;
    private static final int QUEEN = 4;
    private static final int KING = 5;

    // 3D Table: [color][pieceType][square]
    // Colors: 0 = White, 1 = Black
    // Piece Types: 0 = Pawn, 1 = Knight, 2 = Bishop, 3 = Rook, 4 = Queen, 5 = King
    private final long[][][] pieceKeys;
    // white or black
    private final long sideToMoveKey;
    // 4 possible castles - 2^4 = 16
    private final long[] castlingRightsKeys;
    // 8 pawns
    private final long[] enPassantKeys;
    // Record score for each position
    private final Map<Long, TranspositionTableEntry> boardToScoreTable;

    public TranspositionTable() {
        Random rng = new Random(123456789);

        // populate the map with the random keys
        pieceKeys = new long[2][6][64];
        for (int color = 0; color < 2; color++) {
            for (int piece = 0; piece < 6; piece++) {
                for (int square = 0; square < 64; square++) {
                    pieceKeys[color][piece][square] = rng.nextLong();
                }
            }
        }

        // auxiliary keys
        sideToMoveKey = rng.nextLong();

        castlingRightsKeys = new long[16];
        for (int i = 0; i < 16; i++) {
            castlingRightsKeys[i] = rng.nextLong();
        }

        enPassantKeys = new long[8];
        for (int i = 0; i < 8; i++) {
            enPassantKeys[i] = rng.nextLong();
        }

        // concurrent map
        boardToScoreTable = new ConcurrentHashMap<>();
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
     * Computes Zobrist hash for current game state
     * @param state game state
     * @return hash
     */
    private long hash(GameState state) {
        long hash = 0L;

        // hash board
        for (int idx = 0; idx < 64; idx++) {
            Piece piece = state.getPieceAtIndex(idx);
            if (piece == null) continue;
            hash ^= getPieceKey(piece, idx);
        }

        // hash current side
        if (!state.isWhiteTurn()) {
            hash ^= sideToMoveKey;
        }

        // hash castling rights
        int castleRights = 0;
        if (state.isWhiteKingsideCastlingAllowed())  castleRights |= 1;
        if (state.isWhiteQueensideCastlingAllowed()) castleRights |= 2;
        if (state.isBlackKingsideCastlingAllowed())  castleRights |= 4;
        if (state.isBlackQueensideCastlingAllowed()) castleRights |= 8;
        hash ^= castlingRightsKeys[castleRights];

        // en passant hash
        String enPassantSquare = state.getEnPassantSquare();
        if (enPassantSquare != null && !enPassantSquare.isEmpty()) {
            int enPassantFile = enPassantSquare.charAt(0) - 'a'; //a=0, h=7
            if (enPassantFile >= 0 && enPassantFile < 8) {
                hash ^= enPassantKeys[enPassantFile];
            }
        }

        return hash;
    }

    /**
     * Converts piece to it's seed
     * @param piece
     * @param boardIndex
     * @return
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
        } else {
            type = KING;
        }

        return pieceKeys[color][type][boardIndex];
    }

}
