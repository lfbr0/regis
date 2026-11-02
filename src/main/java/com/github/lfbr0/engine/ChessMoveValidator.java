package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/1/26
 * @project regis
 */

import com.github.lfbr0.board.ChessGameState;
import com.github.lfbr0.board.ChessPiece;
import com.github.lfbr0.board.piece.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generates legal moves for a given position and verifies if a move is legal
 */
public class ChessMoveValidator {

    private ChessMoveValidator() {
        // prevent instantiation
    }

    /**
     * Returns all legal moves for current side
     * @param state state of chess game
     * @return set of all legal moves
     */
    public static Set<ChessMove> getLegalMoves(ChessGameState state) {
        return IntStream
                .range(0, 64) // for all indices, check all pieces
                .filter(idx -> {
                    // check that we are grabbing a piece and it's our color
                    ChessPiece piece = state.getBoard()[idx];
                    return piece != null && piece.isWhite() == state.isWhiteTurn();
                })
                .mapToObj(idx -> new Object[]{ idx, state.getBoard()[idx] }) // idx, piece pair
                .flatMap(idxPiecePair -> { // convert pair to chess move
                    int idx = (int) idxPiecePair[0];
                    ChessPiece piece = (ChessPiece) idxPiecePair[1];

                    return piece
                            .getMoveOffsets()
                            .stream()
                            .map(offset -> idx + offset)
                            .filter(to -> to >= 0 && to < 64) // filter moves out of board
                            .flatMap(to -> {
                                // if pawn is moving to last rank, it can promote
                                boolean promotion = piece instanceof PawnPiece && to / 8 == (piece.isWhite() ? 7 : 0);

                                if (promotion) {
                                    return Stream
                                            // promotable pieces
                                            .of(BishopPiece.class, KnightPiece.class, RookPiece.class, QueenPiece.class)
                                            .map(type -> new ChessMove(idx, to, type));
                                }

                                return Stream.of(new ChessMove(idx, to, null));
                    });
                })
                .filter(move -> isLegalMove(state, move)) // get only legal moves
                .collect(Collectors.toSet());
    }

    /**
     * Verifies that move is legal and can be done
     * @param state current chess game state
     * @param move candidate move
     * @return true if legal move
     */
    public static boolean isLegalMove(ChessGameState state, ChessMove move) {

        // is it inside the board?
        if (move.getFrom() < 0 || move.getFrom() >= 64) {
            return false;
        }

        ChessPiece movingPiece = state.getBoard()[move.getFrom()];

        // does it point to an empty square?
        if (movingPiece == null) {
            return false;
        }

        // first check if colors agree - in other words, black can't move white & vice-versa
        if (movingPiece.isWhite() != state.isWhiteTurn()) {
            return false;
        }

        // check if this move is possible for the piece (can we move piece this many squares?)
        int moveOffset = move.getTo() - move.getFrom();
        if (!movingPiece.getMoveOffsets().contains(moveOffset)) {
            return false;
        }

        // are we moving to a place already occupied by us?
        ChessPiece targetPiece = state.getBoard()[move.getTo()];
        if (targetPiece != null && targetPiece.isWhite() == state.isWhiteTurn()) {
            return false;
        }

        // are we trying to capture a king?
        if (targetPiece instanceof KingPiece) {
            return false;
        }

        // check file/rank differences. An allowed offset alone can wrap across board edges.
        if (!validGeometry(movingPiece, move.getFrom(), move.getTo())) {
            return false;
        }

        /**
         * Pawn rules:
         * - Forward destination must be empty.
         * - Double advance requires the starting rank and both squares empty.
         * - Diagonal movement requires an enemy piece or valid en passant.
         * - En passant must remove the adjacent enemy pawn when simulating.
         */
        // TODO

        // bishops, rooks, and queens cannot jump over pieces. Knights can. - path is clear?
        // TODO

        // required when a pawn reaches the last rank; forbidden otherwise and for other pieces.
        // TODO

        // requires the correct right, king and rook on their starting squares, and an empty path.
        // the king cannot start in, pass through, or land in check.
        // TODO

        // simulate move, then reject it if king is checked - board is copied to not alter state
        // TODO

        return true;
    }

    /**
     * This checks if the move is valid for the piece and has the right geometry.
     * For example, h1 and a2 are one offset away, you cannot wrap around the board.
     * @param movingPiece moving piece
     * @param from starting square
     * @param to ending square
     * @return true if move has valid geometry
     */
    private static boolean validGeometry(ChessPiece movingPiece, int from, int to) {
        int offset = to - from;

        // rule to remember: idx % 8 = file
        // rule to remember: idx / 8 = rank because 0/8 = 0, 1/8 = 1, 16/8 = 2, etc.

        if (movingPiece instanceof PawnPiece) {
            // if on first file, cannot move left since destination wraps around
            if (from % 8 == 0 && to % 8 == 7) {
                return false;
            }
            // if on last file, cannot move right wraps around
            if (from % 8 == 7 && to % 8 == 0) {
                return false;
            }
        }
        else if (movingPiece instanceof KnightPiece) {
            // cannot move out side the board to left if not at least 2 squares away
            if ((offset == 6 || offset == -10) && from % 8 < 2) {
                return false;
            }
            // cannot move out side the board to right if not at least 2 squares away
            if ((offset == -6 || offset == 10) && from % 8 > 5) {
                return false;
            }
            // cannot move out right side the board to top if not at least 2 square away
            if (offset == 17 && (from > 46 || from % 8 > 6)) { // idx 46 = g6
                return false;
            }
            // cannot move out left side the board to bottom if not at least 2 square away
            if (offset == 15 && (from % 8 < 1 || from > 47)) { // idx 47 = h6
                return false;
            }
            // cannot move out right side the board to bottom if not at least 2 square away
            if (offset == -15 && (from % 8 > 6 || from < 16)) { // idx 16 = a3
                return false;
            }
            // cannot move out left side the board to bottom if not at least 2 squares away
            if (offset == -17 && (from % 8 < 1 || from < 17)) { // idx 17 = b3
                return false;
            }
        }
        else if (movingPiece instanceof BishopPiece) {
            // file goes from 0 (a) to 7 (h)
            int originFile = from % 8;
            // how many squares can we move before crossing either side?
            int filesToLeft = originFile;
            int filesToRight = 7 - originFile;

            // moving upward right: each diagonal step adds 9 to the index
            if (offset > 0 && offset % 9 == 0 && offset / 9 <= 7) {
                int steps = offset / 9;
                // would we cross the right edge?
                if (steps > filesToRight) {
                    return false;
                }
            }

            // moving upward left: each diagonal step adds 7 to the index
            if (offset > 0 && offset % 7 == 0 && offset / 7 <= 7) {
                int steps = offset / 7;
                // would we cross the left edge?
                if (steps > filesToLeft) {
                    return false;
                }
            }

            // moving downward right: each diagonal step subtracts 7 from the index
            if (offset < 0 && offset % -7 == 0 && offset / -7 <= 7) {
                int steps = offset / -7;
                // would we cross the right edge?
                if (steps > filesToRight) {
                    return false;
                }
            }

            // moving downward left: each diagonal step subtracts 9 from the index
            if (offset < 0 && offset % -9 == 0 && offset / -9 <= 7) {
                int steps = offset / -9;
                // would we cross the left edge?
                if (steps > filesToLeft) {
                    return false;
                }
            }
        }
        else if (movingPiece instanceof RookPiece) {
            // file goes from 0 (a) to 7 (h)
            int originFile = from % 8;
            // how many squares can we move before crossing either side?
            int filesToLeft = originFile;
            int filesToRight = 7 - originFile;

            boolean sameRank = from / 8 == to / 8;
            boolean sameFile = originFile == to % 8;

            // are we moving outside of rank and outside of file? (diagonally)
            if (!sameRank && !sameFile) {
                return false;
            }
            // are we moving left? and more than we can?
            if (sameRank && offset < 0 && Math.abs(offset) > filesToLeft) {
                return false;
            }
            // are we moving right? and moving more than we can?
            else if (sameRank && offset > 0 && Math.abs(offset) > filesToRight) {
                return false;
            }
        }
        else if (movingPiece instanceof QueenPiece) {
            boolean sameRank = from / 8 == to / 8;
            boolean sameFile = from % 8 == to % 8;

            // queens can move horizontally or vertically, like rooks
            if (sameRank || sameFile) {
                return true;
            }

            // otherwise, must move diagonally, like bishops
            int filesMoved = Math.abs(to % 8 - from % 8);
            int ranksMoved = Math.abs(to / 8 - from / 8);

            if (filesMoved != ranksMoved) {
                return false;
            }
        }
        else if (movingPiece instanceof KingPiece) {
            // cannot go left on first file
            if (offset == -1 && from % 8 == 0) {
                return false;
            }
            // cannot go right on last file
            if (offset == 1 && from % 8 == 7) {
                return false;
            }
            // cannot go down on first rank
            if (offset == -8 && from < 8) { // idx 8 = a2
                return false;
            }
            // cannot go up on last rank
            if (offset == 8 && from > 55) { // idx 55 = h7
                return false;
            }
            // cannot go diagonally up left
            if (offset == 7 && (from % 8 == 0 || from == 57)) { // idx 57 = b8
                return false;
            }
            // cannot go diagonally up right
            if (offset == 9 && (from % 8 == 7 || from == 62)) { // idx 62 = g8
                return false;
            }
            // cannot go diagonally down left
            if (offset == -9 && (from % 8 == 0 || from == 1)) { // idx 1 = b1
                return false;
            }
            // cannot go diagonally down right
            if (offset == -7 && (from % 8 == 7 || from == 6)) { // idx 6 = g1
                return false;
            }
            // castling checks on same rank
            if (Math.abs(offset) == 2 && from / 8 != to / 8) {
                return false;
            }
        }
        else {
            throw new IllegalArgumentException("Invalid piece type: " + movingPiece.getClass());
        }

        // no issues found
        return true;
    }

}
