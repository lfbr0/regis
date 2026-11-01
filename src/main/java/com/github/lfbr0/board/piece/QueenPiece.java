package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class QueenPiece extends ChessPiece {
    public QueenPiece(boolean isWhite) {
        super(isWhite, 9.0f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "Q" : "q";
    }
}
