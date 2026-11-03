package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.KingPiece;
import com.github.lfbr0.move.MoveValidator;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * This is the actual engine of Regis - the workhorse. Computes:
 * - best move
 * - best N moves
 * - evaluation of board
 */
public class Engine {

    private ExecutorService executorService;

    public Engine() {
        executorService = newFixedThreadPool(getRuntime().availableProcessors());
    }

    /**
     * This stops all searches currently running
     * And also clears stale old data
     */
    public void resetEngine() {
        executorService.shutdownNow();
        executorService = newFixedThreadPool(getRuntime().availableProcessors());
    }

    /**
     * This stops all searches currently running
     */
    public void shutdown() {
        executorService.shutdownNow();
    }

    /**
     * Evaluates legal moves concurrently, sharing one deadline.
     * Stops early when a root move proves a forced checkmate.
     * @return completed evaluations, best first; possibly fewer than count
     */
    public EngineMove[] bestMoves(GameState state, int count, int depth, long timeoutMillis) {

        if (state == null || count < 0 || depth < 1 || timeoutMillis <= 0 || timeoutMillis > Long.MAX_VALUE / 1_000_000L) {
            throw new IllegalArgumentException("Invalid search arguments");
        }

        if (count == 0) {
            return new EngineMove[0];
        }

        // all evaluators get the same deadline and mate signal
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        AtomicBoolean mateFound = new AtomicBoolean(false);

        // evaluation function is shared by all evaluators
        BiFunction<GameState, Boolean, Double> evalFunction = this::evaluate;

        List<EngineMoveEvaluator> tasks = MoveValidator.getLegalMoves(state)
                .stream()
                .map(move -> new EngineMoveEvaluator(
                        state,
                        move,
                        depth,
                        deadline,
                        mateFound,
                        evalFunction
                ))
                .toList();

        List<EngineMove> results = new ArrayList<>();

        try {
            // move generation already consumed part of our time budget
            long remainingNanos = Math.max(0, deadline - System.nanoTime());

            List<Future<Optional<EngineMove>>> futures = executorService.invokeAll(
                    tasks,
                    remainingNanos,
                    TimeUnit.MILLISECONDS
            );

            for (Future<Optional<EngineMove>> future : futures) {
                if (future.isDone() && !future.isCancelled()) {
                    future.get().ifPresent(results::add);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Engine search failed", e.getCause());
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(EngineMove::evaluation).reversed()) // best result first
                .limit(count)
                .toArray(EngineMove[]::new);
    }

    /**
     * Evaluates the board state and returns a score.
     * @param state current game state
     * @param playerIsWhite true if the player is white, false otherwise
     * @return
     */
    public double evaluate(GameState state, boolean playerIsWhite) {
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
}
