package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class RookPiece extends ChessPiece {

    public RookPiece(boolean isWhite) {
        super(isWhite, 5.0f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "R" : "r";
    }
}
