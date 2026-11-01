package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

import java.util.Set;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class KingPiece extends ChessPiece {

    private static Set<Integer> MOVE_OFFSETS = Set.of(-9, -8, -7, -2, -1, 1, 2, 7, 8, 9);

    public KingPiece(boolean isWhite) {
        super(isWhite, Float.MAX_VALUE);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "K" : "k";
    }

    @Override
    public Set<Integer> getMoveOffsets() {
        return MOVE_OFFSETS;
    }
}
