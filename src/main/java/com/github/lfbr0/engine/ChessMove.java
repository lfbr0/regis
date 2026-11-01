package com.github.lfbr0.engine;

import com.github.lfbr0.board.ChessPiece;
import com.github.lfbr0.board.piece.*;
import lombok.Getter;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/1/26
 * @project regis
 */

public final class ChessMove {

    public static final String CHESS_MOVE_UCI_REGEX = "^[a-h][1-8][a-h][1-8][qrbn]?$";

    // move from where
    @Getter
    private final int from;
    // move to where
    @Getter
    private final int to;
    // promotion piece, if there's a promotion
    private final Class<? extends ChessPiece> promotionPieceClass;

    public ChessMove(String uciMoveString) {
        if (uciMoveString == null || !uciMoveString.matches(CHESS_MOVE_UCI_REGEX)) {
            throw new IllegalArgumentException("Invalid move string: " + uciMoveString);
        }

        from = ChessMove.squareStringToIndex(uciMoveString.substring(0, 2));
        to = ChessMove.squareStringToIndex(uciMoveString.substring(2, 4));

        if (from == to) {
            throw new IllegalArgumentException("Invalid move: " + uciMoveString);
        }

        // if there's a promotion piece, decode it
        if (uciMoveString.length() == 5) {
            char promotionPieceChar = Character.toUpperCase(uciMoveString.charAt(4));
            promotionPieceClass = switch (promotionPieceChar) {
                case 'Q' -> QueenPiece.class;
                case 'R' -> RookPiece.class;
                case 'B' -> BishopPiece.class;
                case 'N' -> KnightPiece.class;
                default -> throw new IllegalArgumentException("Invalid promotion piece: " + promotionPieceChar);
            };
        } else {
            promotionPieceClass = null;
        }
    }

    public ChessMove(int from, int to, Class<? extends ChessPiece> promotionPieceClass) {
        if (from < 0 || from > 63) {
            throw new IllegalArgumentException("Invalid from index: " + from);
        }
        this.from = from;

        if (to < 0 || to > 63) {
            throw new IllegalArgumentException("Invalid to index: " + to);
        }
        this.to = to;

        if (from == to) {
            throw new IllegalArgumentException("Invalid move: " + from + " -> " + to);
        }

        if (promotionPieceClass == KingPiece.class || promotionPieceClass == PawnPiece.class) {
            throw new IllegalArgumentException("Invalid promotion piece: " + promotionPieceClass);
        }
        this.promotionPieceClass = promotionPieceClass;
    }

    public ChessMove(int from, int to) {
        this(from, to, null);
    }

    /**
     * Converts a square string (e.g. "a1") to an index (0 to 63)
     * @param squareStr Square string to convert (e.g. "a1")
     * @return Index of the square string (0 to 63 - 0 = a1, 1 = b1, ...)
     */
    public static int squareStringToIndex(String squareStr) {
        if (squareStr == null || !squareStr.matches("[a-h][1-8]")) {
            throw new IllegalArgumentException("Invalid square string: " + squareStr);
        }
        return (squareStr.charAt(1) - '1') * 8 + squareStr.charAt(0) - 'a';
    }

    /**
     * Converts an index to a square string (e.g. "a1")
     * @param index Index to convert (0 to 63)
     * @return Square string representation of the index (e.g. "a1")
     */
    public static String indexToSquareString(int index) {
        if (index < 0 || index > 63) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }

        return "" + (char) ('a' + index % 8)
                + (char) ('1' + index / 8);
    }

    /**
     * Gets the promotion piece class, if there's a promotion.
     * @return Optional containing the promotion piece class, if there's a promotion.
     */
    public Optional<Class<? extends ChessPiece>> getPromotionPieceClass() {
        return Optional.ofNullable(promotionPieceClass);
    }

    /**
     * Gets the UCI move string representation of this move.
     * @return UCI move string representation of this move.
     */
    public String getUciMoveString() {
        String promotion = "";
        if (promotionPieceClass == QueenPiece.class) promotion = "q";
        else if (promotionPieceClass == RookPiece.class) promotion = "r";
        else if (promotionPieceClass == BishopPiece.class) promotion = "b";
        else if (promotionPieceClass == KnightPiece.class) promotion = "n";

        return ChessMove.indexToSquareString(from) + ChessMove.indexToSquareString(to) + promotion;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessMove chessMove)) return false;
        return  Objects.equals(from, chessMove.from) &&
                Objects.equals(to, chessMove.to) &&
                Objects.equals(promotionPieceClass, chessMove.promotionPieceClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, promotionPieceClass);
    }
}