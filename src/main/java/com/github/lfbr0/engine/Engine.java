package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.KingPiece;
import com.github.lfbr0.move.Move;
import com.github.lfbr0.move.MoveHandler;
import com.github.lfbr0.move.MoveValidator;

import java.util.Comparator;
import java.util.Set;

/**
 * This is the actual engine of Regis - the workhorse. Computes:
 * - best move
 * - best N moves
 * - evaluation of board
 */
public class Engine {

    private static final double CHECKMATE_REWARD_VALUE = 100_000;
    private static final double STALEMATE_REWARD_VALUE = 0;

    /**
     * Returns an ordered array of the best moves for the given state.
     * First element is the best move.
     * @param state current game state
     * @return array of best n moves
     */
    public static EngineMove[] bestMoves(GameState state, int count, int depth) {
        if (state == null || count < 0 || depth < 1) {
            throw new IllegalArgumentException("State must be != null, count must be > 0, and depth must be >= 1");
        }

        boolean playerIsWhite = state.isWhiteTurn();
        Set<Move> allLegalMoves = MoveValidator.getLegalMoves(state);

        return allLegalMoves
                .stream()
                .map(move -> {
                    // apply move, then search from the resulting position
                    GameState nextState = MoveHandler.applyMove(state, move);
                    double evaluation = evaluateDepth(nextState, playerIsWhite, depth - 1);
                    return new EngineMove(move, evaluation);
                })
                // highest score first, assuming scores favor the original player
                .sorted(Comparator.comparingDouble(EngineMove::evaluation).reversed())
                .limit(count)
                .toArray(EngineMove[]::new);
    }

    /**
     * Evaluates the board state and returns a score.
     * @param state current game state
     * @param playerIsWhite true if the player is white, false otherwise
     * @return
     */
    public static double evaluate(GameState state, boolean playerIsWhite) {
        if (state == null) {
            throw new IllegalArgumentException("Chess game state cannot be null");
        }

        // for now, start by just evaluating material
        double materialScore = 0;
        for (int i = 0; i < 64; i++) {
            Piece piece = state.getPieceAtIndex(i);

            // don't count our king and empty squares
            if (piece == null || piece instanceof KingPiece) {
                continue;
            }

            // if both bools same value, it's the same color piece as player so it's "+"
            materialScore += (piece.isWhite() == playerIsWhite) ? piece.getValue() : -piece.getValue();
        }

        return materialScore;
    }


    /**
     * AUXILIARY METHODS
     */


    /**
     * Auxiliary method to evaluate the board state at a given depth.
     * @param currentState current game state
     * @param playerIsWhite true if the player is white, false otherwise
     * @param remainingDepth remaining depth
     * @return evaluation score
     */
    private static double evaluateDepth(GameState currentState, boolean playerIsWhite, int remainingDepth) {
        // otherwise, get all legal moves and evaluate them, always assume best moves
        Set<Move> allLegalMoves = MoveValidator.getLegalMoves(currentState);

        // are we out of moves? possible checkmate or stalemate
        if (allLegalMoves.isEmpty()) {
            // if not in check, it's stalemate
            if (!MoveValidator.isInCheck(currentState)) {
                return STALEMATE_REWARD_VALUE;
            }

            // otherwise it's checkmate. if our turn, we lost!
            return currentState.isWhiteTurn() == playerIsWhite
                    ? -CHECKMATE_REWARD_VALUE
                    : CHECKMATE_REWARD_VALUE;
        }

        // we have no more depth to explore, so just evaluate the board
        if (remainingDepth == 0) {
            return evaluate(currentState, playerIsWhite);
        }

        // we must maximize our score, and the opponent must minimize it
        boolean playersTurn = currentState.isWhiteTurn() == playerIsWhite;
        double bestScore = playersTurn ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;

        for (Move move : allLegalMoves) {
            GameState nextState = MoveHandler.applyMove(currentState, move);
            double evaluation = evaluateDepth(nextState, playerIsWhite, remainingDepth - 1);

            // our turn: highest score; opponent's turn: lowest score
            bestScore = playersTurn
                    ? Math.max(bestScore, evaluation)
                    : Math.min(bestScore, evaluation);
        }

        return bestScore;
    }

}
