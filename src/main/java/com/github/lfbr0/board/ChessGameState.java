package com.github.lfbr0.board;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */

/**
 * This represents the Chess Board and it's state
 */
@Data
@AllArgsConstructor
public class ChessGameState {

    // 64 squares, read from bottom left to top right
    // so a1 = 0, a2 = 8, h1 = 7, h8 = 63
    private final ChessPiece[] board;
    // marks whose turn it is
    private final boolean isWhiteTurn;
    // castling rights
    private final boolean whiteKingsideCastling;
    private final boolean whiteQueensideCastling;
    private final boolean blackKingsideCastling;
    private final boolean blackQueensideCastling;
    // algebraic square, such as "e3", or null when absent
    private final String enPassantSquare;
    // halfmove needed for the 50-move draw claim and automatic 75-move draw
    // counts individual turns since the last pawn move or capture; either resets to zero
    private final int halfmoveClock;
    // records the number of full moves
    private final int fullmoveNumber;


    public ChessGameState(ChessPiece[] board, boolean isWhiteTurn) {
        this.board = board;
        this.isWhiteTurn = isWhiteTurn;
        this.whiteKingsideCastling = false;
        this.whiteQueensideCastling = false;
        this.blackKingsideCastling = false;
        this.blackQueensideCastling = false;
        this.enPassantSquare = null;
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
    }

    /**
     * Creates a new ChessGameState from a FEN string.
     * @param fenString FEN string
     */
    public ChessGameState(String fenString) {
        ChessGameState state = FenCodec.decode(fenString);
        this.board = state.getBoard();
        this.isWhiteTurn = state.isWhiteTurn();
        this.whiteKingsideCastling = state.isWhiteKingsideCastling();
        this.whiteQueensideCastling = state.isWhiteQueensideCastling();
        this.blackKingsideCastling = state.isBlackKingsideCastling();
        this.blackQueensideCastling = state.isBlackQueensideCastling();
        this.enPassantSquare = state.getEnPassantSquare();
        this.halfmoveClock = state.getHalfmoveClock();
        this.fullmoveNumber = state.getFullmoveNumber();
    }
}
