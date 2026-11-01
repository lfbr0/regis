package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class RookPiece extends ChessPiece {

    private static final Set<Integer> MOVE_OFFSETS;

    static {
        Set<Integer> offsets = new HashSet<>();

        // move accross file and rank
        for (int distance = 1; distance <= 7; distance++) {
            offsets.add(distance);
            offsets.add(-distance);
            offsets.add(distance * 8);
            offsets.add(-distance * 8);
        }

        MOVE_OFFSETS = Set.copyOf(offsets);
    }

    public RookPiece(boolean isWhite) {
        super(isWhite, 5.0f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "R" : "r";
    }

    @Override
    public Set<Integer> getMoveOffsets() {
        return MOVE_OFFSETS;
    }
}
