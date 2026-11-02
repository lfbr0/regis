package com.github.lfbr0;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.engine.Engine;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;

import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * This is the entry point of the application.
 * It starts with UCI prompt for usage...
 */
public class RegisUciMain {

    public static void main( String[] args ) {
        Scanner scanner = new Scanner(System.in);

        // to run searches in parallel
        ExecutorService executorService = newFixedThreadPool(getRuntime().availableProcessors());

        // will be initialized when necessaty
        GameState currentGameState;
        Engine engine = new Engine(executorService);

        // UCI loop
        while (true) {

            // get command from stdin
            String command = scanner
                    .nextLine()
                    .trim();

            boolean shouldExit = interpretUciCommand(command);

            if (shouldExit) {
                break;
            }
        }

        executorService.shutdown();
        scanner.close();
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

        return false;
    }
}
