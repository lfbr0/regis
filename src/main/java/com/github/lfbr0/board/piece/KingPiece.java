package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class KingPiece extends ChessPiece {
    public KingPiece(boolean isWhite) {
        super(isWhite, Float.MAX_VALUE);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "K" : "k";
    }
}
