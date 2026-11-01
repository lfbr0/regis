package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class PawnPiece extends ChessPiece {

    public PawnPiece(boolean isWhite) {
        super(isWhite, 1.0f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "P" : "p";
    }
}
