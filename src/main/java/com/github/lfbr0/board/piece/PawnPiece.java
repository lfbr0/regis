package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

import java.util.Set;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class PawnPiece extends ChessPiece {

    private static final Set<Integer> WHITE_MOVE_OFFSETS = Set.of(7, 8, 9, 16);
    private static final Set<Integer> BLACK_MOVE_OFFSETS = Set.of(-7, -8, -9, -16);

    public PawnPiece(boolean isWhite) {
        super(isWhite, 1.0f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "P" : "p";
    }

    @Override
    public Set<Integer> getMoveOffsets() {
        return isWhite() ? WHITE_MOVE_OFFSETS : BLACK_MOVE_OFFSETS;
    }
}
