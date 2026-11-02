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
import com.github.lfbr0.move.MoveValidator;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * This is the actual engine of Regis - the workhorse. Computes:
 * - best move
 * - best N moves
 * - evaluation of board
 */
public class Engine {

    private static final ExecutorService ENGINE_EXECUTOR_SERVICE = newFixedThreadPool(getRuntime().availableProcessors());

    /**
     * Evaluates legal moves concurrently, sharing one deadline.
     * Stops early when a root move proves a forced checkmate.
     * @return completed evaluations, best first; possibly fewer than count
     */
    public static EngineMove[] bestMoves(GameState state, int count, int depth, long timeoutMillis) {

        if (state == null || count < 0 || depth < 1 || timeoutMillis <= 0 || timeoutMillis > Long.MAX_VALUE / 1_000_000L) {
            throw new IllegalArgumentException("Invalid search arguments");
        }

        if (count == 0) {
            return new EngineMove[0];
        }

        // all evaluators get the same deadline and mate signal
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        AtomicBoolean mateFound = new AtomicBoolean(false);

        // each search gets its own completion queue
        CompletionService<Optional<EngineMove>> completed = new ExecutorCompletionService<>(ENGINE_EXECUTOR_SERVICE);

        List<Future<Optional<EngineMove>>> futures = new ArrayList<>();
        List<EngineMove> results = new ArrayList<>();

        try {
            // for each legal move, evaluate it on parallel
            for (Move move : MoveValidator.getLegalMoves(state)) {
                if (System.nanoTime() - deadline >= 0) {
                    break;
                }

                // boot up parallel evaluation for this move
                futures.add(completed.submit(new EngineMoveEvaluator(state, move, depth, deadline, mateFound)));
            }

            for (int received = 0; received < futures.size(); received++) {
                // if time has expired, still collect results already queued
                long timeLeft = Math.max(0, deadline - System.nanoTime());

                Future<Optional<EngineMove>> finished = completed.poll(timeLeft, TimeUnit.NANOSECONDS);

                if (finished == null) {
                    break;
                }

                // stopped evaluators don't provide a usable score
                finished.get().ifPresent(results::add);

                // don't break merely because mateFound is true:
                // the evaluator with the mating move may still be publishing
                // its result into the completion queue
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Engine search failed", e.getCause());
        } finally {
            // stop any remaining work when collection ends
            futures.forEach(future -> future.cancel(true));
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(EngineMove::evaluation).reversed()) // best result first
                .limit(count)
                .toArray(EngineMove[]::new);
    }

    /**
     * Call when the application no longer needs the engine.
     */
    public static void shutdown() {
        ENGINE_EXECUTOR_SERVICE.shutdownNow();
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
}
