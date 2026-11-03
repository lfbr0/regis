package com.github.lfbr0;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.engine.Engine;
import com.github.lfbr0.move.Move;
import com.github.lfbr0.move.MoveHandler;
import com.github.lfbr0.move.MoveValidator;

import java.util.Locale;
import java.util.Scanner;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * This is the entry point of the application.
 * It starts with UCI prompt for usage...
 */
public class RegisUciMain {

    // will be initialized when necessaty
    private final static Engine ENGINE = new Engine();
    private static GameState GAME_STATE = new GameState();

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


                boolean shouldExit = interpretUciCommand(command);
                if (shouldExit) {
                    break;
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid command: " + e.getMessage());
        } finally {
            scanner.close();
            ENGINE.shutdown();
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
            System.out.println("id name Regis");
            System.out.println("id author Luís Filipe Ribeiro");
            System.out.println("uciok");
            return false;
        }
        // is it ready
        if (command.equals("isready")) {
            System.out.println("readyok");
        }
        // start new game by resetting the board & engine
        if (command.equals("ucinewgame")) {
            GAME_STATE = new GameState();
            ENGINE.resetEngine();
        }
        // position commands
        if (command.startsWith("position ")) {
            setPosition(command.substring("position ".length()));
        }

        return false;
    }

    /**
     * set board position after a position command.
     * @param command rest of the command
     */
    private static void setPosition(String command) {
        int movesIndex = command.indexOf(" moves ");
        GameState nextState;

        // set board with default position
        if (command.startsWith("startpos ")) {
            nextState = new GameState();
        }
        // set board with FEN string
        else if (command.startsWith("fen")) {
            String fen = command.substring(
                    "fen ".length(),
                    movesIndex == -1 ? command.length() : movesIndex
            );
            nextState = new GameState(fen);
        } else {
            throw new IllegalArgumentException("Invalid position command: " + command);
        }

        // if no moves to apply
        if (movesIndex == -1) {
            return;
        }

        // apply moves now
        String[] uciMoves = command
                .substring(movesIndex + " moves ".length())
                .trim()
                .split("\\s+");

        // apply moves
        for (String uciMove : uciMoves) {
            Move move = new Move(uciMove.trim());

            if (!MoveValidator.isLegalMove(nextState, move)) {
                throw new IllegalArgumentException("Invalid move: " + uciMove);
            }

            // update game state by move
            nextState = MoveHandler.applyMove(nextState, move);
        }

        // commit game state
        GAME_STATE = nextState;
    }
}
