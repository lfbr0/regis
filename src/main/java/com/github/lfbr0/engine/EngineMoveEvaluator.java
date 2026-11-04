package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.move.Move;
import com.github.lfbr0.move.MoveHandler;
import com.github.lfbr0.move.MoveValidator;
import lombok.AllArgsConstructor;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

/**
 * Evaluates one root move and its continuations. Actually performs the evaluation.
 * For efficiency, will stop early if mateFound by some other thread.
 */
@AllArgsConstructor
public class EngineMoveEvaluator implements Callable<Optional<EngineMove>> {

    private static final double CHECKMATE_REWARD_VALUE = 100_000;
    private static final double STALEMATE_REWARD_VALUE = 0;

    private final GameState state;
    private final Move move;
    private final int depth;
    private final long deadline;
    private final BiFunction<GameState, Boolean, Double> evaluationFunction; // game state, is white -> result

    @Override
    public Optional<EngineMove> call() throws Exception {
        try {
            shouldStopSearch();

            boolean playerIsWhite = state.isWhiteTurn();
            GameState nextState = MoveHandler.applyMove(state, move);
            double score = evaluateDepth(
                    nextState,
                    playerIsWhite,
                    depth - 1,
                    Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY);

            return Optional.of(new EngineMove(move, score));
        } catch (CancellationException ignored) {
            // this root move wasn't fully evaluated
            return Optional.empty();
        }
    }

    /**
     * Auxiliary method to evaluate the board state at a given depth.
     * @param currentState current game state
     * @param playerIsWhite true if the player is white, false otherwise
     * @param remainingDepth remaining depth
     * @param alpha alpha value for alpha beta pruning
     * @param beta beta value for alpha beta pruning
     * @return evaluation score
     */
    private double evaluateDepth(GameState currentState,
                                 boolean playerIsWhite,
                                 int remainingDepth,
                                 double alpha,
                                 double beta) {

        // check cancellation before exploring this position
        shouldStopSearch();

        // otherwise, get all legal moves and evaluate them, always assume best moves
        Set<Move> allLegalMoves = MoveValidator.getLegalMoves(currentState);

        // generating moves may have consumed the remaining time
        shouldStopSearch();

        // are we out of moves? possible checkmate or stalemate
        if (allLegalMoves.isEmpty()) {
            // if not in check, it's stalemate
            if (!MoveValidator.isInCheck(currentState)) {
                return STALEMATE_REWARD_VALUE;
            }

            // otherwise it's checkmate. if our turn, we lost!
            // add remaining depth to value early checkmates or punish getting early checkmated
            double checkmateReward = CHECKMATE_REWARD_VALUE + (remainingDepth * 1000);
            // if our turn, we got checkmated! negative reward = punishment
            return currentState.isWhiteTurn() == playerIsWhite ? -checkmateReward : checkmateReward;
        }

        // we have no more depth to explore, so just evaluate the board
        if (remainingDepth == 0) {
            return evaluationFunction.apply(currentState, playerIsWhite);
        }

        // we must maximize our score, and the opponent must minimize it
        boolean playersTurn = currentState.isWhiteTurn() == playerIsWhite;
        double bestScore = playersTurn ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;

        for (Move move : allLegalMoves) {
            // generating moves may have consumed the remaining time
            shouldStopSearch();

            GameState nextState = MoveHandler.applyMove(currentState, move);
            double evaluation = evaluateDepth(nextState, playerIsWhite, remainingDepth - 1, alpha, beta);

            // our turn: highest score; opponent's turn: lowest score
            if (playersTurn) {
                bestScore = Math.max(bestScore, evaluation);
                alpha = Math.max(alpha, evaluation);
            } else {
                bestScore = Math.min(bestScore, evaluation);
                beta = Math.min(beta, evaluation);
            }

            // stop if other side has option just as good
            if (alpha >= beta) {
                break;
            }
        }

        return bestScore;
    }

    /**
     * Stops search if mate found or deadline reached.
     */
    private void shouldStopSearch() {
        if (Thread.currentThread().isInterrupted() || System.nanoTime() - deadline >= 0) {
            throw new CancellationException("Evaluation stopped");
        }
    }
}
