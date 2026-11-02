package com.github.lfbr0.move;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/2/26
 * @project regis
 */

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.*;

/**
 * Applies a move to the game state
 */
public class MoveHandler {

    private MoveHandler() {
        // prevent instantiation
    }

    /**
     * Applies a move to the game state. ASSUMES MOVE IS VALID.
     * Applies following logic:
     * - if capture, removes captured piece
     * - if promotion, promotes piece
     * - if en passant, removes pawn
     * - if castling, applies castling
     * - otherwise, moves piece
     * @param state current game state
     * @param move move to apply
     * @return new game state
     */
    public static GameState applyMove(GameState state, Move move) {
        if (state == null || move == null) {
            throw new IllegalArgumentException("Invalid arguments, null passed to applyMove!");
        }

        Piece movingPiece = state.getPieceAtIndex(move.getFrom());

        if (movingPiece instanceof PawnPiece) {
            return applyPawnMove(state, move);
        } else if (movingPiece instanceof KingPiece) {
            return applyKingMove(state, move);
        } else {
            return applyPieceMove(state, move);
        }
    }

    /**
     * Applies a piece move.
     * If capture, removes captured piece. Else, moves piece.
     * @param state current game state
     * @param move move to apply
     * @return new game state
     */
    private static GameState applyPieceMove(GameState state, Move move) {
        Piece movingPiece = state.getPieceAtIndex(move.getFrom());
        Piece capturedPiece = state.getPieceAtIndex(move.getTo());
        Piece[] newBoard = state.getBoardCopy(); // it already returns new copy

        // capture or move
        newBoard[move.getTo()] = movingPiece;
        newBoard[move.getFrom()] = null;
        // change turn to next side
        boolean isWhiteTurn = state.isWhiteTurn();

        // castling vars
        boolean whiteKingsideCastlingAllowed = state.isWhiteKingsideCastlingAllowed();
        boolean whiteQueensideCastlingAllowed = state.isWhiteQueensideCastlingAllowed();
        boolean blackKingsideCastlingAllowed = state.isBlackKingsideCastlingAllowed();
        boolean blackQueensideCastlingAllowed = state.isBlackQueensideCastlingAllowed();

        // did we move a rook and had castle?
        if (movingPiece instanceof RookPiece) {
            // disable castling for white king side
            if (isWhiteTurn) {
                if (move.getFrom() == 0 && whiteQueensideCastlingAllowed) {
                    whiteQueensideCastlingAllowed = false;
                }
                if (move.getFrom() == 7 && whiteKingsideCastlingAllowed) {
                    whiteKingsideCastlingAllowed = false;
                }
            }
            // disable castling for black king side
            else {
                if (move.getFrom() == 56 && blackQueensideCastlingAllowed) {
                    blackQueensideCastlingAllowed = false;
                }
                if (move.getFrom() == 63 && blackKingsideCastlingAllowed) {
                    blackKingsideCastlingAllowed = false;
                }
            }
        }

        // did we capture a rook on its starting square?
        if (capturedPiece instanceof RookPiece) {
            if (capturedPiece.isWhite()) {
                if (move.getTo() == 0) {
                    whiteQueensideCastlingAllowed = false;
                }
                if (move.getTo() == 7) {
                    whiteKingsideCastlingAllowed = false;
                }
            } else {
                if (move.getTo() == 56) {
                    blackQueensideCastlingAllowed = false;
                }
                if (move.getTo() == 63) {
                    blackKingsideCastlingAllowed = false;
                }
            }
        }

        return new GameState(
                newBoard,
                !isWhiteTurn,
                whiteKingsideCastlingAllowed,
                whiteQueensideCastlingAllowed,
                blackKingsideCastlingAllowed,
                blackQueensideCastlingAllowed,
                null, // no en passant movement done
                capturedPiece != null ? 0 : state.getHalfmoveClock() + 1,
                state.getFullmoveNumber() + (isWhiteTurn ? 0 : 1)
        );
    }

    /**
     * Applies a king move. Mostly the same as a piece move, but with some extra logic for castling.
     * @param state current game state
     * @param move move to apply
     * @return new game state
     */
    private static GameState applyKingMove(GameState state, Move move) {
        Piece[] newBoard = state.getBoardCopy();

        // castling vars
        boolean whiteKingsideCastlingAllowed = state.isWhiteKingsideCastlingAllowed();
        boolean whiteQueensideCastlingAllowed = state.isWhiteQueensideCastlingAllowed();
        boolean blackKingsideCastlingAllowed = state.isBlackKingsideCastlingAllowed();
        boolean blackQueensideCastlingAllowed = state.isBlackQueensideCastlingAllowed();

        // check if castling move, and if so, move rook only (king will be moved by applyPieceMove)
        // white castle queenside
        if (state.isWhiteTurn() && move.getFrom() == 4 && move.getTo() == 2) {
            // move rook
            newBoard[3] = newBoard[0];
            newBoard[0] = null;
        }
        // white castle kingside
        else if (state.isWhiteTurn() && move.getFrom() == 4 && move.getTo() == 6) {
            // move rook
            newBoard[5] = newBoard[7];
            newBoard[7] = null;
        }
        // black castle queenside
        else if (!state.isWhiteTurn() && move.getFrom() == 60 && move.getTo() == 58) {
            // move rook
            newBoard[59] = newBoard[56];
            newBoard[56] = null;
        }
        // black castle kingside
        else if (!state.isWhiteTurn() && move.getFrom() == 60 && move.getTo() == 62) {
            // move rook
            newBoard[61] = newBoard[63];
            newBoard[63] = null;
        }

        // any king move permanently removes both castling rights for its side
        if (state.isWhiteTurn()) {
            whiteKingsideCastlingAllowed = false;
            whiteQueensideCastlingAllowed = false;
        } else {
            blackKingsideCastlingAllowed = false;
            blackQueensideCastlingAllowed = false;
        }

        // new state with new position of rooks if necessary
        GameState newState = new GameState(
                newBoard,
                state.isWhiteTurn(),
                whiteKingsideCastlingAllowed,
                whiteQueensideCastlingAllowed,
                blackKingsideCastlingAllowed,
                blackQueensideCastlingAllowed,
                state.getEnPassantSquare(),
                state.getHalfmoveClock(),
                state.getFullmoveNumber()
        );

        // still move king
        return applyPieceMove(newState, move);
    }

    /**
     * Applies a pawn move.
     * Considers capture, en passant, and promotion.
     * @param state current game state
     * @param move move to apply, assumed valid
     * @return new game state
     */
    private static GameState applyPawnMove(GameState state, Move move) {
        int from = move.getFrom();
        int to = move.getTo();
        boolean isWhite = state.getPieceAtIndex(from).isWhite();

        // check before moving, since the destination will contain our pawn afterward
        // we're moving ranks and there's nothing there, so we can capture en passant
        boolean enPassant = from % 8 != to % 8 && state.getPieceAtIndex(to) == null;

        // apply ordinary movement, captures, castling rights, turn, and move counters
        GameState movedState = applyPieceMove(state, move);
        Piece[] newBoard = movedState.getBoardCopy();

        // en passant captures the pawn behind the destination
        if (enPassant) {
            int capturedPawnIndex = isWhite ? to - 8 : to + 8;
            newBoard[capturedPawnIndex] = null;
        }

        // if promoting, replace the pawn with the selected piece
        if (move.getPromotionPieceClass().isPresent()) {
            Class<? extends Piece> promotion = move.getPromotionPieceClass().orElseThrow();

            if (promotion == QueenPiece.class) {
                newBoard[to] = new QueenPiece(isWhite);
            } else if (promotion == RookPiece.class) {
                newBoard[to] = new RookPiece(isWhite);
            } else if (promotion == BishopPiece.class) {
                newBoard[to] = new BishopPiece(isWhite);
            } else if (promotion == KnightPiece.class) {
                newBoard[to] = new KnightPiece(isWhite);
            } else {
                throw new IllegalArgumentException("Invalid promotion piece: " + promotion);
            }
        }

        // only a double advance creates a new en passant target
        String enPassantSquare = null;

        if (Math.abs(to - from) == 16) {
            int crossedSquare = (from + to) / 2;
            enPassantSquare = Move.indexToSquareString(crossedSquare);
        }

        return new GameState(
                newBoard,
                movedState.isWhiteTurn(),
                movedState.isWhiteKingsideCastlingAllowed(),
                movedState.isWhiteQueensideCastlingAllowed(),
                movedState.isBlackKingsideCastlingAllowed(),
                movedState.isBlackQueensideCastlingAllowed(),
                enPassantSquare,
                0, // every pawn move resets the halfmove clock
                movedState.getFullmoveNumber()
        );
    }

}
