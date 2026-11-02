package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */

import com.github.lfbr0.board.ChessGameState;
import com.github.lfbr0.board.ChessPiece;
import com.github.lfbr0.board.piece.*;

import java.util.Arrays;

/**
 * Applies a move to the game state
 */
public class ChessMoveHandler {

    private ChessMoveHandler() {
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
    public ChessGameState applyMove(ChessGameState state, ChessMove move) {
        if (state == null || move == null) {
            throw new IllegalArgumentException("Invalid arguments, null passed to applyMove!");
        }

        ChessPiece movingPiece = state.getBoard()[move.getFrom()];

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
    private ChessGameState applyPieceMove(ChessGameState state, ChessMove move) {
        ChessPiece movingPiece = state.getPieceAtIndex(move.getFrom());
        ChessPiece[] newBoard = state.getBoardCopy(); // it already returns new copy

        // capture or move
        newBoard[move.getTo()] = movingPiece;
        // change turn to next side
        boolean isWhiteTurn = !state.isWhiteTurn();

        // did we move a rook and had castle?
        if (movingPiece instanceof RookPiece) {
            // TODO
        }

        return new ChessGameState(
                newBoard,
                isWhiteTurn,
                // TODO all args

        );

        return null; // TODO
    }

    /**
     * Applies a king move. Mostly the same as a piece move, but with some extra logic for castling.
     * @param state current game state
     * @param move move to apply
     * @return new game state
     */
    private ChessGameState applyKingMove(ChessGameState state, ChessMove move) {
        return null; // TODO
    }

    /**
     * Applies a pawn move.
     * Considers capture, en passant, and promotion.
     * @param state
     * @param move
     * @return
     */
    private ChessGameState applyPawnMove(ChessGameState state, ChessMove move) {
        return null; // TODO
    }

}
