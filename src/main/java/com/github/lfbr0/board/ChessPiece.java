package com.github.lfbr0.board;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */

import java.util.Set;

/**
 * Represents generic chess pieces
 */
public abstract class ChessPiece {

    private final boolean isWhite;
    private final float value;

    public ChessPiece(boolean isWhite, float value) {
        this.isWhite = isWhite;
        this.value = value;
    }

    /**
     * Returns the symbol for this piece in FEN notation.
     * @return symbol for this piece in FEN notation.
     */
    public abstract String toSymbol();

    /**
     * Returns a list of possible moves for this piece.
     * Each move is represented by an index change in the board.
     * @return set of possible moves represented by index changes in the board
     */
    public abstract Set<Integer> getMoveOffsets();

    public float getValue() {
        return value;
    }

    public boolean isWhite() {
        return isWhite;
    }
}
