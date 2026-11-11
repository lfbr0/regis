package com.github.lfbr0;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.engine.Engine;
import com.github.lfbr0.engine.EngineMove;
import com.github.lfbr0.move.Move;
import com.github.lfbr0.move.MoveHandler;
import com.github.lfbr0.move.MoveValidator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the entry point of the application.
 * Reads UCI commands from stdin and sends replies through stdout.
 */
public class RegisUciMain {

    private final static Engine ENGINE = new Engine();
    private static GameState GAME_STATE = new GameState();

    // this thread calls the engine, which already manages its own workers
    private static Thread SEARCH_THREAD;

    // protects search ownership and bestmove publication
    private final static Object SEARCH_LOCK = new Object();

    public static void main( String[] args ) {
        Scanner scanner = new Scanner(System.in);

        try {
            // UCI loop
            while (scanner.hasNextLine()) {

                // get command from stdin
                String command = scanner
                        .nextLine()
                        .trim()
                        .replaceAll("\\s+", " ");

                if (command.isEmpty()) {
                    continue;
                }

                try {
                    if (interpretUciCommand(command)) {
                        break;
                    }
                } catch (IllegalArgumentException e) {
                    // bad input shouldn't stop the application
                    System.err.println("Invalid command: " + e.getMessage());
                }
            }
        } finally {
            // don't publish results when the application is closing
            stopSearch(false);
            ENGINE.shutdown();
            scanner.close();
        }
    }

    /**
     * Interprets a UCI command.
     * @param command UCI command
     * @return true if the application should exit
     */
    private static boolean interpretUciCommand(String command) {
        if (command.equals("quit")) {
            return true;
        }

        // send basic info
        if (command.equals("uci")) {
            send("id name Regis");
            send("id author Luís Filipe Ribeiro");
            send("uciok");
            return false;
        }

        // this must also work while the engine is searching
        if (command.equals("isready")) {
            send("readyok");
            return false;
        }

        // stop previous work before resetting the engine
        if (command.equals("ucinewgame")) {
            stopSearch(false);

            GAME_STATE = new GameState();
            ENGINE.resetEngine();
            return false;
        }

        // position commands
        if (command.equals("position") || command.startsWith("position ")) {
            setPosition(command.substring("position".length()).trim());
            return false;
        }

        // search commands
        if (command.equals("go") || command.startsWith("go ")) {
            startSearch(command.substring("go".length()).trim());
            return false;
        }

        // stop search and publish its best available move
        if (command.equals("stop")) {
            stopSearch(true);
            return false;
        }

        // ignore commands we don't support yet
        return false;
    }

    /**
     * Sets board position after a position command.
     * @param command rest of the command
     */
    private static void setPosition(String command) {
        String[] tokens = command.split("\\s+");
        GameState nextState;
        int index;

        // set board with default position
        if (tokens[0].equals("startpos")) {
            nextState = new GameState();
            index = 1;
        }
        // set board with six FEN fields
        else if (tokens[0].equals("fen") && tokens.length >= 7) {
            String fen = String.join(" ", Arrays.copyOfRange(tokens, 1, 7));
            nextState = new GameState(fen);
            index = 7;
        } else {
            throw new IllegalArgumentException("Expected startpos or six FEN fields");
        }

        // apply optional moves locally
        if (index < tokens.length) {
            if (!tokens[index].equals("moves")) {
                throw new IllegalArgumentException("Expected moves keyword");
            }

            for (index++; index < tokens.length; index++) {
                Move move = new Move(tokens[index]);

                if (!MoveValidator.isLegalMove(nextState, move)) {
                    throw new IllegalArgumentException(
                            "Invalid move: " + tokens[index]);
                }

                nextState = MoveHandler.applyMove(nextState, move);
            }
        }

        // don't allow the old search to publish for the new position
        stopSearch(false);

        // commit only after the entire command succeeds
        GAME_STATE = nextState;
    }

    /**
     * Starts a search for best move without blocking the command loop.
     * @param command depth and/or movetime arguments
     */
    private static void startSearch(String command) {
        int depth = Integer.MAX_VALUE;
        long timeoutMillis = Long.MAX_VALUE / 1_000_000L;

        // -1 means the GUI didn't provide this limit
        long moveTime = -1;
        long whiteTime = -1;
        long blackTime = -1;

        // increments are added after completing a move
        long whiteIncrement = 0;
        long blackIncrement = 0;

        // estimate when the GUI doesn't specify the next time control
        int movesToGo = 30;
        boolean depthProvided = false;

        if (command.isBlank()) {
            throw new IllegalArgumentException("Expected go depth N or go movetime N");
        }

        String[] tokens = command.split("\\s+");

        // parse the limits supported by our engine
        for (int index = 0; index < tokens.length; index++) {
            String argument = tokens[index];

            if (!argument.equals("depth")
                    && !argument.equals("movetime")
                    && !argument.equals("wtime")
                    && !argument.equals("btime")
                    && !argument.equals("winc")
                    && !argument.equals("binc")
                    && !argument.equals("movestogo")) {
                throw new IllegalArgumentException(
                        "Unsupported go argument: " + argument);
            }

            if (++index >= tokens.length) {
                throw new IllegalArgumentException("Missing value for " + argument);
            }

            long value = Long.parseLong(tokens[index]);

            if (value < 0) {
                throw new IllegalArgumentException(argument + " cannot be negative");
            }

            if (argument.equals("depth")) {
                if (value < 1 || value > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Invalid depth");
                }

                depth = (int) value;
                depthProvided = true;
            }

            if (argument.equals("movetime")) {
                if (value < 1 || value > Long.MAX_VALUE / 1_000_000L) {
                    throw new IllegalArgumentException("Invalid movetime");
                }

                moveTime = value;
            }

            if (argument.equals("wtime")) {
                whiteTime = value;
            }

            if (argument.equals("btime")) {
                blackTime = value;
            }

            if (argument.equals("winc")) {
                whiteIncrement = value;
            }

            if (argument.equals("binc")) {
                blackIncrement = value;
            }

            if (argument.equals("movestogo")) {
                if (value < 1 || value > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Invalid movestogo");
                }

                movesToGo = (int) value;
            }
        }

        // use the clock belonging to the side about to move
        boolean whiteToMove = GAME_STATE.isWhiteTurn();
        long remainingTime = whiteToMove ? whiteTime : blackTime;
        long increment = whiteToMove ? whiteIncrement : blackIncrement;

        if (remainingTime >= 0) {
            timeoutMillis = calculateTimeBudget(remainingTime, increment, movesToGo);
        }

        // apply an explicit movetime limit as well
        if (moveTime >= 0) {
            timeoutMillis = Math.min(timeoutMillis, moveTime);
        }

        // increments alone don't provide a usable search limit
        if (!depthProvided && moveTime < 0 && remainingTime < 0) {
            throw new IllegalArgumentException(
                    "Expected depth, movetime, or a clock for the side to move");
        }

        stopSearch(false);

        // capture the position and limits for this search
        GameState state = GAME_STATE;
        int searchDepth = depth;
        long searchTimeout = timeoutMillis;

        // keep a legal move if interrupted before any evaluation finishes
        Move fallback = MoveValidator.getLegalMoves(state)
                .stream()
                .min(Comparator.comparing(Move::getUciMoveString))
                .orElse(null);

        Thread thread = new Thread(() -> {
            Move bestMove = fallback;
            boolean usedFallback = fallback != null;

            try {
                // stop may arrive before this thread starts running
                if (!Thread.currentThread().isInterrupted() && fallback != null) {
                    // get best move
                    EngineMove[] results = ENGINE.bestMoves(state, 1, searchDepth, searchTimeout);

                    if (results.length > 0) {
                        bestMove = results[0].move();
                        usedFallback = false;
                    }
                }
            } catch (RuntimeException e) {
                System.err.println("Search failed: " + e.getMessage());
            } finally {
                synchronized (SEARCH_LOCK) {
                    // only the current search may publish a result
                    if (SEARCH_THREAD == Thread.currentThread()) {
                        // if we used a fallback move, log it
                        if (usedFallback) {
                            sendInfo("Using legal fallback move; no search result available");
                        }

                        send("bestmove " + (bestMove == null ? "0000" : bestMove.getUciMoveString()));
                        SEARCH_THREAD = null;
                    }
                }
            }
        }, "regis-search");

        synchronized (SEARCH_LOCK) {
            SEARCH_THREAD = thread;
            thread.start();
        }
    }

    /**
     * Calculates how much time we can spend on the current move.
     * @param remainingTime time left on our clock, in milliseconds
     * @param increment time added after each move, in milliseconds
     * @param movesToGo moves until the next time control, or our estimate
     * @return search budget in milliseconds
     */
    private static long calculateTimeBudget(long remainingTime, long increment, int movesToGo) {

        // leave time for move generation, scheduling, and sending bestmove
        long reserve = Math.min(100, remainingTime / 10);

        // cap the budget to the range accepted by Engine.bestMoves
        long available = Math.min(
                remainingTime - reserve,
                Long.MAX_VALUE / 1_000_000L);

        // spread our remaining time across the expected moves
        long base = Math.min(available, remainingTime / movesToGo);

        // use part of the increment without exceeding our current clock
        long extra = Math.min(available - base, increment / 2);

        // the engine requires a positive timeout, even with an exhausted clock
        return Math.max(1, base + extra);
    }

    /**
     * Stops the current search.
     * @param publishResult true for stop, false when replacing or closing
     */
    private static void stopSearch(boolean publishResult) {
        Thread thread;

        synchronized (SEARCH_LOCK) {
            thread = SEARCH_THREAD;

            // remove ownership to suppress obsolete results
            if (!publishResult) {
                SEARCH_THREAD = null;
            }

            if (thread != null) {
                thread.interrupt();
            }
        }

        // wait outside the lock so the search can publish and finish
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Sends one complete reply to the GUI.
     * @param message UCI reply
     */
    private static synchronized void send(String message) {
        System.out.println(message);
        System.out.flush();
    }

    /**
     * Sends one info reply to the GUI.
     * @param message UCI info reply
     */
    private static void sendInfo(String message) {
        send("info string " + message);
    }
}