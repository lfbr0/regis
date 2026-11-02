package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.*;

/**
 * Applies a move to the game state
 */
public class MoveHandler {

    private MoveHandler() {
        // prevent instantiation
    }

    /**
     * Applies a move to the game state. ASSUMES MOVE IS VALID.
     * Applies following logic:
     * - if capture, removes captured piece
     * - if promotion, promotes piece
     * - if en passant, removes pawn
     * - if castling, applies castling
     * - otherwise, moves piece
     * @param state current game state
     * @param move move to apply
     * @return new game state
     */
    public GameState applyMove(GameState state, Move move) {
        if (state == null || move == null) {
            throw new IllegalArgumentException("Invalid arguments, null passed to applyMove!");
        }

        Piece movingPiece = state.getPieceAtIndex(move.getFrom());

        if (movingPiece instanceof PawnPiece) {
            return applyPawnMove(state, move);
        } else if (movingPiece instanceof KingPiece) {
            return applyKingMove(state, move);
        } else {
            return applyPieceMove(state, move);
        }
    }

    /**
     * Applies a piece move.
     * If capture, removes captured piece. Else, moves piece.
     * @param state current game state
     * @param move move to apply
     * @return new game state
     */
    private GameState applyPieceMove(GameState state, Move move) {
        Piece movingPiece = state.getPieceAtIndex(move.getFrom());
        Piece[] newBoard = state.getBoardCopy(); // it already returns new copy

        // capture or move
        newBoard[move.getTo()] = movingPiece;
        // change turn to next side
        boolean isWhiteTurn = !state.isWhiteTurn();

        // did we move a rook and had castle?
        if (movingPiece instanceof RookPiece) {
            // TODO
        }

        return null; // TODO
    }

    /**
     * Applies a king move. Mostly the same as a piece move, but with some extra logic for castling.
     * @param state current game state
     * @param move move to apply
     * @return new game state
     */
    private GameState applyKingMove(GameState state, Move move) {
        return null; // TODO
    }

    /**
     * Applies a pawn move.
     * Considers capture, en passant, and promotion.
     * @param state
     * @param move
     * @return
     */
    private GameState applyPawnMove(GameState state, Move move) {
        return null; // TODO
    }

}
