package com.github.lfbr0.board;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */

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

    public abstract String toSymbol();

    public float getValue() {
        return value;
    }

    public boolean isWhite() {
        return isWhite;
    }
}
