package com.github.lfbr0.board;

import lombok.*;

import java.util.Arrays;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */

/**
 * This represents the Chess Board and it's state
 */
@AllArgsConstructor
@ToString
public class GameState {

    // 64 squares, read from bottom left to top right
    // so a1 = 0, a2 = 8, h1 = 7, h8 = 63
    private final Piece[] board;
    // marks whose turn it is
    @Getter private final boolean isWhiteTurn;
    // castling rights
    @Getter private final boolean whiteKingsideCastlingAllowed;
    @Getter private final boolean whiteQueensideCastlingAllowed;
    @Getter private final boolean blackKingsideCastlingAllowed;
    @Getter private final boolean blackQueensideCastlingAllowed;
    // algebraic square, such as "e3", or null when absent
    @Getter private final String enPassantSquare;
    // halfmove needed for the 50-move draw claim and automatic 75-move draw
    // counts individual turns since the last pawn move or capture; either resets to zero
    @Getter private final int halfmoveClock;
    // records the number of full moves
    @Getter private final int fullmoveNumber;


    public GameState(Piece[] board, boolean isWhiteTurn) {
        this.board = board;
        this.isWhiteTurn = isWhiteTurn;
        this.whiteKingsideCastlingAllowed = false;
        this.whiteQueensideCastlingAllowed = false;
        this.blackKingsideCastlingAllowed = false;
        this.blackQueensideCastlingAllowed = false;
        this.enPassantSquare = null;
        this.halfmoveClock = 0;
        this.fullmoveNumber = 1;
    }

    /**
     * Creates a new ChessGameState from a FEN string.
     * @param fenString FEN string
     */
    public GameState(String fenString) {
        GameState state = FenCodec.decode(fenString);
        this.board = state.getBoardCopy();
        this.isWhiteTurn = state.isWhiteTurn();
        this.whiteKingsideCastlingAllowed = state.isWhiteKingsideCastlingAllowed();
        this.whiteQueensideCastlingAllowed = state.isWhiteQueensideCastlingAllowed();
        this.blackKingsideCastlingAllowed = state.isBlackKingsideCastlingAllowed();
        this.blackQueensideCastlingAllowed = state.isBlackQueensideCastlingAllowed();
        this.enPassantSquare = state.getEnPassantSquare();
        this.halfmoveClock = state.getHalfmoveClock();
        this.fullmoveNumber = state.getFullmoveNumber();
    }

    /**
     * Returns a copy of the board.
     * @return copy of the board
     */
    public Piece[] getBoardCopy() {
        return Arrays.copyOf(board, 64);
    }

    /**
     * Returns the piece at the given index.
     * @param idx Index of the piece to return
     * @return Piece at the given index
     */
    public Piece getPieceAtIndex(int idx) {
        return board[idx];
    }
}
