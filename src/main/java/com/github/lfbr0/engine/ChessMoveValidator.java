package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/1/26
 * @project regis
 */

import com.github.lfbr0.board.ChessGameState;
import com.github.lfbr0.board.ChessPiece;
import com.github.lfbr0.board.piece.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
                .parallel() // parallelize it
                .filter(idx -> state.getBoard()[idx] != null) // only check pieces
                .mapToObj(idx -> new Object[]{ idx, state.getBoard()[idx] }) // idx, piece pair
                .flatMap(idxPiecePair -> { // convert pair to chess move
                    int idx = (int) idxPiecePair[0];
                    ChessPiece piece = (ChessPiece) idxPiecePair[1];

                    // for each move possible, convert it
                    // but first assume possibility of being a passed pawn and promotion next turn (on penultimate row)
                    if (idx >= 48 && idx <= 55 && piece instanceof PawnPiece) {
                        return Stream
                                // pieces that the pawn can promote to
                                .of(BishopPiece.class, KnightPiece.class, RookPiece.class, QueenPiece.class)
                                // map to all possible promotions
                                .map(promoClass -> new ChessMove(idx, idx+1, promoClass));
                    }
                    // otherwise just do for all moves without promotion
                    else {
                        return piece
                                .getMoveOffsets()
                                .stream()
                                .map(offset -> new ChessMove(idx, idx + offset, null));
                    }
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

        // check file/rank differences. An allowed offset alone can wrap across board edges.
        // TODO

        // bishops, rooks, and queens cannot jump over pieces. Knights can.
        // TODO

        /**
         * Pawn rules:
         * - Forward destination must be empty.
         * - Double advance requires the starting rank and both squares empty.
         * - Diagonal movement requires an enemy piece or valid en passant.
         * - En passant must remove the adjacent enemy pawn when simulating.
         */
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

}
