package com.github.lfbr0.engine;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.*;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/12/26
 * @project regis
 */
public class GameStateEvaluator {

    private static final double CENTER_CONTROL_BONUS = 0.03;
    private static final double DEVELOPMENT_BONUS = 0.08;
    private static final double BISHOP_PAIR_BONUS = 0.25;
    private static final double OPEN_FILE_BONUS = 0.15;
    // fractions of the threatened piece's material value
    private static final double UNDEFENDED_THREAT_PENALTY_COEF = 0.25;
    private static final double PAWN_THREAT_PENALTY_COEF = 0.10;

    /**
     * Evaluates the board state and returns a score.
     * @param state current game state
     * @param playerIsWhite true if the player is white, false otherwise
     * @return balance from the player's perspective
     */
    public static double evaluate(GameState state, boolean playerIsWhite) {
        if (state == null) {
            throw new IllegalArgumentException("Chess game state cannot be null");
        }

        // every helper evaluates from white's perspective
        double score = evaluateMaterial(state)
                + evaluateCenterControl(state)
                + evaluateDevelopment(state)
                + evaluateBishopPair(state)
                + evaluateOpenFiles(state)
                + evaluatePieceSafety(state);

        // if we're black, reverse the balance
        return playerIsWhite ? score : -score;
    }


    /**
     * UTILITY METHODS
     */


    /**
     * Evaluates the material balance.
     * @param state current game state
     * @return material balance from white's perspective
     */
    private static double evaluateMaterial(GameState state) {
        double score = 0;

        for (int i = 0; i < 64; i++) {
            Piece piece = state.getPieceAtIndex(i);

            // don't count kings and empty squares
            if (piece == null || piece instanceof KingPiece) {
                continue;
            }

            // white pieces add and black pieces subtract
            score += piece.isWhite() ? piece.getValue() : -piece.getValue();
        }

        return score;
    }

    /**
     * Rewards pieces attacking the four central squares.
     * @param state current game state
     * @return center control balance from white's perspective
     */
    private static double evaluateCenterControl(GameState state) {
        // d4, e4, d5, e5
        int[] centralSquares = {27, 28, 35, 36};
        double score = 0;

        for (int from = 0; from < 64; from++) {
            Piece piece = state.getPieceAtIndex(from);

            if (piece == null) {
                continue;
            }

            for (int to : centralSquares) {
                // count each piece controlling the center
                if (attacksSquare(state, piece, from, to)) {
                    score += piece.isWhite() ? CENTER_CONTROL_BONUS : -CENTER_CONTROL_BONUS;
                }
            }
        }

        return score;
    }

    /**
     * Rewards knights and bishops outside their home rank.
     * The bonus fades as material disappears.
     * @param state current game state
     * @return development balance from white's perspective
     */
    private static double evaluateDevelopment(GameState state) {
        double score = 0;
        int remainingPhase = 0;

        for (int i = 0; i < 64; i++) {
            Piece piece = state.getPieceAtIndex(i);

            if (piece == null) {
                continue;
            }

            // count remaining material to estimate the game phase
            if (piece instanceof QueenPiece) {
                remainingPhase += 4;
            } else if (piece instanceof RookPiece) {
                remainingPhase += 2;
            } else if (piece instanceof KnightPiece || piece instanceof BishopPiece) {
                remainingPhase++;

                // minor pieces outside their home rank count as developed
                int homeRank = piece.isWhite() ? 0 : 7;

                if (i / 8 != homeRank) {
                    score += piece.isWhite() ? DEVELOPMENT_BONUS : -DEVELOPMENT_BONUS;
                }
            }
        }

        // starting material gives a phase of 24
        // promotions can increase it, so don't allow a weight above 1
        double openingWeight = Math.min(1.0, remainingPhase / 24.0);
        return score * openingWeight;
    }

    /**
     * Rewards having bishops covering both square colors.
     * @param state current game state
     * @return bishop pair balance from white's perspective
     */
    private static double evaluateBishopPair(GameState state) {
        boolean whiteFirstColor = false;
        boolean whiteSecondColor = false;
        boolean blackFirstColor = false;
        boolean blackSecondColor = false;

        for (int i = 0; i < 64; i++) {
            Piece piece = state.getPieceAtIndex(i);

            if (!(piece instanceof BishopPiece)) {
                continue;
            }

            // alternating files and ranks give alternating square colors
            boolean firstColor = (i / 8 + i % 8) % 2 == 0;

            if (piece.isWhite()) {
                if (firstColor) {
                    whiteFirstColor = true;
                } else {
                    whiteSecondColor = true;
                }
            } else {
                if (firstColor) {
                    blackFirstColor = true;
                } else {
                    blackSecondColor = true;
                }
            }
        }

        double score = 0;

        // two bishops on the same square color don't count as a pair
        if (whiteFirstColor && whiteSecondColor) {
            score += BISHOP_PAIR_BONUS;
        }
        if (blackFirstColor && blackSecondColor) {
            score -= BISHOP_PAIR_BONUS;
        }

        return score;
    }

    /**
     * Rewards rooks on files without any pawns.
     * @param state current game state
     * @return open file balance from white's perspective
     */
    private static double evaluateOpenFiles(GameState state) {
        double score = 0;

        for (int i = 0; i < 64; i++) {
            Piece piece = state.getPieceAtIndex(i);

            if (!(piece instanceof RookPiece)) {
                continue;
            }

            boolean openFile = true;

            // check every square on the rook's file
            for (int square = i % 8; square < 64; square += 8) {
                // a pawn of either color means the file isn't open
                if (state.getPieceAtIndex(square) instanceof PawnPiece) {
                    openFile = false;
                    break;
                }
            }

            if (openFile) {
                score += piece.isWhite() ? OPEN_FILE_BONUS : -OPEN_FILE_BONUS;
            }
        }

        return score;
    }

    /**
     * Penalizes undefended attacked pieces and pieces threatened by pawns.
     * This estimates danger; it doesn't calculate whether a capture wins material.
     * @param state current game state
     * @return piece safety balance from white's perspective
     */
    private static double evaluatePieceSafety(GameState state) {
        double score = 0;

        for (int target = 0; target < 64; target++) {
            Piece piece = state.getPieceAtIndex(target);

            // king safety is evaluated separately
            if (piece == null || piece instanceof KingPiece) {
                continue;
            }

            boolean attacked = false;
            boolean defended = false;
            boolean attackedByPawn = false;

            for (int from = 0; from < 64; from++) {
                Piece other = state.getPieceAtIndex(from);

                if (other == null || from == target) {
                    continue;
                }

                if (!attacksSquare(state, other, from, target)) {
                    continue;
                }

                if (other.isWhite() == piece.isWhite()) {
                    defended = true;
                } else {
                    attacked = true;

                    if (other instanceof PawnPiece) {
                        attackedByPawn = true;
                    }
                }

                // we already know everything needed for this piece
                if (defended && attackedByPawn) {
                    break;
                }
            }

            double penalty = 0;

            // an attacked piece without defenders deserves a larger penalty
            if (attacked && !defended) {
                penalty += piece.getValue() * UNDEFENDED_THREAT_PENALTY_COEF;
            }

            // pawn threats matter even when a more valuable piece is defended
            // pawn versus pawn threats don't receive this extra penalty
            if (attackedByPawn && !(piece instanceof PawnPiece)) {
                penalty += piece.getValue() * PAWN_THREAT_PENALTY_COEF;
            }

            // danger to white subtracts; danger to black adds
            score += piece.isWhite() ? -penalty : penalty;
        }

        return score;
    }

    /**
     * Checks whether a piece attacks or defends a square.
     * This doesn't check whether moving would expose its king.
     * @param state current game state
     * @param piece attacking piece
     * @param from piece's square
     * @param to square to check
     * @return true if the piece controls the square
     */
    private static boolean attacksSquare(GameState state, Piece piece, int from, int to) {
        // a piece doesn't attack its own square
        if (from == to) {
            return false;
        }

        int fileDifference = to % 8 - from % 8;
        int rankDifference = to / 8 - from / 8;
        int files = Math.abs(fileDifference);
        int ranks = Math.abs(rankDifference);

        if (piece instanceof PawnPiece) {
            // pawns attack diagonally, even when the destination is empty
            int direction = piece.isWhite() ? 1 : -1;
            return files == 1 && rankDifference == direction;
        }

        if (piece instanceof KnightPiece) {
            // two squares in one direction and one in the other
            return (files == 2 && ranks == 1)
                    || (files == 1 && ranks == 2);
        }

        if (piece instanceof KingPiece) {
            // castling isn't an attack
            return files <= 1 && ranks <= 1;
        }

        boolean diagonal = files == ranks;
        boolean straight = fileDifference == 0 || rankDifference == 0;

        if (piece instanceof BishopPiece) {
            if (!diagonal) {
                return false;
            }
        } else if (piece instanceof RookPiece) {
            if (!straight) {
                return false;
            }
        } else if (piece instanceof QueenPiece) {
            if (!diagonal && !straight) {
                return false;
            }
        } else {
            throw new IllegalArgumentException("Invalid piece type: " + piece.getClass());
        }

        // move one square at a time toward the destination
        int step = Integer.signum(rankDifference) * 8
                + Integer.signum(fileDifference);

        for (int square = from + step; square != to; square += step) {
            // sliding pieces cannot attack through another piece
            if (state.getPieceAtIndex(square) != null) {
                return false;
            }
        }

        // an occupied destination still counts as attacked or defended
        return true;
    }
}
