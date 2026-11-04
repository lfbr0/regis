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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
     * Requests cancellation of running work and creates a fresh executor.
     */
    public void resetEngine() {
        executorService.shutdownNow();
        executorService = newFixedThreadPool(getRuntime().availableProcessors());
    }

    /**
     * Requests cancellation of running work and shuts down the executor.
     */
    public void shutdown() {
        executorService.shutdownNow();
    }

    /**
     * Evaluates legal moves concurrently, one depth at a time.
     * All iterations and evaluators share the same deadline.
     * @param state current game state
     * @param count maximum number of moves to return
     * @param depth maximum depth in plies
     * @param timeoutMillis total time budget in milliseconds
     * @return best moves from the last completed depth, or an empty array
     *         if there are no legal moves or no depth finished
     */
    public EngineMove[] bestMoves(GameState state, int count, int depth, long timeoutMillis) {
        if (state == null || count < 0 || depth < 1 || timeoutMillis <= 0 || timeoutMillis > Long.MAX_VALUE / 1_000_000L) {
            throw new IllegalArgumentException("Invalid search arguments");
        }

        if (count == 0 || Thread.currentThread().isInterrupted()) {
            return new EngineMove[0];
        }

        // start the time budget before generating the root moves
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);

        Set<Move> legalMoves = MoveValidator.getLegalMoves(state);
        if (legalMoves.isEmpty()) {
            return new EngineMove[0];
        }

        // evaluation function is shared by all evaluators
        BiFunction<GameState, Boolean, Double> evalFunction = this::evaluate;
        List<EngineMove> lastCompleted = List.of();

        // start at depth one so a timed search can return a shallow result
        for (int currentDepth = 1; ; currentDepth++) {
            if (Thread.currentThread().isInterrupted() || deadline - System.nanoTime() <= 0) {
                break;
            }

            List<EngineMoveEvaluator> tasks = new ArrayList<>(legalMoves.size());

            for (Move move : legalMoves) {
                tasks.add(new EngineMoveEvaluator(
                        state,
                        move,
                        currentDepth,
                        deadline,
                        evalFunction
                ));
            }

            // move generation and earlier iterations already used some time
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                break;
            }

            List<EngineMove> results = new ArrayList<>(tasks.size());

            try {
                List<Future<Optional<EngineMove>>> futures = executorService.invokeAll(
                        tasks,
                        remainingNanos,
                        TimeUnit.NANOSECONDS
                );

                for (Future<Optional<EngineMove>> future : futures) {
                    if (!future.isCancelled()) {
                        future.get().ifPresent(results::add);
                    }
                }
            } catch (InterruptedException e) {
                // keep the last completed depth when UCI sends stop
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                throw new IllegalStateException("Engine search failed", e.getCause());
            }

            // a depth is complete only when every root move has a score
            // check before limiting the results to the requested count
            if (results.size() != tasks.size()) {
                break;
            }

            lastCompleted = results;

            // every root move finished at this depth
            System.err.println("Completed depth: " + currentDepth + " plies");

            if (currentDepth == depth) {
                break;
            }
        }

        return lastCompleted.stream()
                .sorted(Comparator.comparingDouble(EngineMove::evaluation).reversed())
                .limit(count)
                .toArray(EngineMove[]::new);
    }

    /**
     * Evaluates the board state and returns a score.
     * @param state current game state
     * @param playerIsWhite true if the player is white, false otherwise
     * @return material balance from the player's perspective
     */
    public double evaluate(GameState state, boolean playerIsWhite) {
        if (state == null) {
            throw new IllegalArgumentException("Chess game state cannot be null");
        }

        // for now, start by just evaluating material
        double materialScore = 0;
        for (int i = 0; i < 64; i++) {
            Piece piece = state.getPieceAtIndex(i);

            // don't count kings and empty squares
            if (piece == null || piece instanceof KingPiece) {
                continue;
            }

            // our pieces add to the score and the opponent's pieces subtract
            materialScore += (piece.isWhite() == playerIsWhite) ? piece.getValue() : -piece.getValue();
        }

        return materialScore;
    }
}