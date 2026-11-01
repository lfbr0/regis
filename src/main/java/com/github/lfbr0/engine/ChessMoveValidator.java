package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/1/26
 * @project regis
 */

import com.github.lfbr0.board.ChessGameState;

import java.util.Set;

/**
 * Generates legal moves for a given position and verifies if a move is legal
 */
public class ChessMoveValidator {

    private ChessMoveValidator() {
        // prevent instantiation
    }

    public static Set<ChessMove> getLegalMoves(ChessGameState state) {
        return null;
    }

    public static boolean isLegalMove(ChessGameState state, ChessMove move) {
        return false;
    }

}
