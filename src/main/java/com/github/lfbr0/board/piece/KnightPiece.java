package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

import java.util.Set;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class KnightPiece extends ChessPiece {

    private static final Set<Integer> MOVE_OFFSETS = Set.of(-17, -15, -10, -6, 6, 10, 15, 17);

    public KnightPiece(boolean isWhite) {
        super(isWhite, 3.0f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "N" : "n";
    }

    @Override
    public Set<Integer> getMoveOffsets() {
        return MOVE_OFFSETS;
    }
}
