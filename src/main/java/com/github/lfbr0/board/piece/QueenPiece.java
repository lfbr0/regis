package com.github.lfbr0.board.piece;

import com.github.lfbr0.board.ChessPiece;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */
public class QueenPiece extends ChessPiece {

    private static final Set<Integer> MOVE_OFFSETS;

    static {
        Set<Integer> offsets = new HashSet<>();

        // move accross file, rank and diagonals
        for (int distance = 1; distance <= 7; distance++) {
            for (int direction : new int[] {1, 7, 8, 9}) {
                offsets.add(distance * direction);
                offsets.add(-distance * direction);
            }
        }

        MOVE_OFFSETS = Set.copyOf(offsets);
    }

    public QueenPiece(boolean isWhite) {
        super(isWhite, 9.0f);
    }

    @Override
    public String toSymbol() {
        return isWhite() ? "Q" : "q";
    }

    @Override
    public Set<Integer> getMoveOffsets() {
        return MOVE_OFFSETS;
    }
}
