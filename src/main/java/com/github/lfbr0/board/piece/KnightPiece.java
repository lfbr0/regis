package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class KnightPiece extends ChessPiece {

    public KnightPiece(boolean isWhite) {
        super(isWhite, 3.0f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "N" : "n";
    }
}
