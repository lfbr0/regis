package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.Piece;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class BishopPiece extends Piece {

    private static final Set<Integer> MOVE_OFFSETS;

    static {
        Set<Integer> offsets = new HashSet<>();

        // move accross diagonals
        for (int distance = 1; distance <= 7; distance++) {
            offsets.add(distance * 7);
            offsets.add(-distance * 7);
            offsets.add(distance * 9);
            offsets.add(-distance * 9);
        }

        MOVE_OFFSETS = Set.copyOf(offsets);
    }

    public BishopPiece(boolean isWhite) {
        super(isWhite, 3.25f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "B" : "b";
    }

    @Override
    public Set<Integer> getMoveOffsets() {
        return MOVE_OFFSETS;
    }
}
