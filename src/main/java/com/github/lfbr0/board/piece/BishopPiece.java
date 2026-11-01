package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class BishopPiece extends ChessPiece {

    public BishopPiece(boolean isWhite) {
        super(isWhite, 3.25f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "B" : "b";
    }
}
