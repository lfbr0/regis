package com.github.lfbr0.engine;

import com.github.lfbr0.board.ChessPiece;
import com.github.lfbr0.board.piece.BishopPiece;
import com.github.lfbr0.board.piece.KnightPiece;
import com.github.lfbr0.board.piece.QueenPiece;
import com.github.lfbr0.board.piece.RookPiece;
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
    private final String from;
    // move to where
    @Getter
    private final String to;
    // promotion piece, if there's a promotion
    private final Class<? extends ChessPiece> promotionPieceClass;

    public ChessMove(String uciMoveString) {
        if (uciMoveString == null || !uciMoveString.matches(CHESS_MOVE_UCI_REGEX)) {
            throw new IllegalArgumentException("Invalid move string: " + uciMoveString);
        }

        from = uciMoveString.substring(0, 2);
        to = uciMoveString.substring(2, 4);

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

        return from + to + promotion;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessMove chessMove)) return false;
        return Objects.equals(from, chessMove.from) && Objects.equals(to, chessMove.to) && Objects.equals(promotionPieceClass, chessMove.promotionPieceClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, promotionPieceClass);
    }
}