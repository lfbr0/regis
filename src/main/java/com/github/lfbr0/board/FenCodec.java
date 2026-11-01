package com.github.lfbr0.board;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 10/31/26
 * @project regis
 */

import com.github.lfbr0.board.piece.*;

/**
 * Encode and decode FEN strings
 */
public class FenCodec {

    // This is the default starting position of the chess board.
    public static final String DEFAULT_FEN_STRING = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // validation regex for FEN rows string
    private static final String FEN_ROWS_REGEX = "^(?:[rnbqkpRNBQKP1-8]+/){7}[rnbqkpRNBQKP1-8]+$";

    public static String encode(ChessGameState state) {
        if (state == null) {
            throw new IllegalArgumentException("Chess game state cannot be null");
        }

        final StringBuilder fenStringBuilder = new StringBuilder();

        // start by enconding board
        fenStringBuilder.append(encodeBoard(state.getBoard()));

        // add active color
        fenStringBuilder.append(" ").append(state.isWhiteTurn() ? "w" : "b");

        return fenStringBuilder.toString();
    }

    public static ChessGameState decode(String fen) {
        if (fen == null) {
            throw new IllegalArgumentException("FEN string cannot be null");
        }

        String[] fenTokens = fen.split(" ");
        if (fenTokens.length < 1) {
            throw new IllegalArgumentException("Please check the FEN string format: " + fen);
        }

        // start by deciphering the rowboard
        String fenRowsString = fenTokens[0];
        if (!fenRowsString.matches(FEN_ROWS_REGEX)) {
            throw new IllegalArgumentException("Invalid FEN rows string: " + fenRowsString);
        }
        ChessPiece[] board = decodeBoard(fenRowsString.split("/"));

        // then, the turn - if no token, assume white
        boolean isWhiteTurn;
        if (fenTokens.length >= 2) {
            String activeColor = fenTokens[1];
            // validate color
            switch (activeColor) {
                case "w": isWhiteTurn = true; break;
                case "b": isWhiteTurn = false; break;
                default: throw new IllegalArgumentException("Invalid active color: " + activeColor);
            }
        } else {
            isWhiteTurn = true;
        }

        return new ChessGameState(board, isWhiteTurn);
    }

    /**
     * Utilities below
     */

    /**
     * Encodes the Chess board to a FEN string rows
     * @param board Chess board
     * @return FEN string rows
     */
    private static StringBuilder encodeBoard(ChessPiece[] board) {
        if (board == null || board.length != 64) {
            throw new IllegalArgumentException("Invalid board");
        }

        StringBuilder boardFenSb = new StringBuilder(72);

        int currentBoardIndex = 56; // start at a8
        while (true) {
            // count empty spaces between pieces
            int emptySpaces = 0;

            for (int i = 0; i < 8; i++) {
                ChessPiece piece = board[currentBoardIndex + i];

                // if no piece, then add empty spaces
                if (piece == null) {
                    emptySpaces++;
                }
                // if piece, then add piece symbol; also, if empty spaces > 0, then add empty spaces first
                else {
                    if (emptySpaces > 0) {
                        boardFenSb.append(emptySpaces);
                        emptySpaces = 0;
                    }
                    boardFenSb.append(piece.toSymbol());
                }
            }

            // if there are still empty spaces, then add them
            if (emptySpaces > 0) {
                boardFenSb.append(emptySpaces);
            }

            // go to next row below
            currentBoardIndex -= 8;

            // if there's more than one row, then add a slash
            if (currentBoardIndex >= 0) {
                boardFenSb.append('/');
            }
            // else, we're done encoding the board
            else break;
        }

        return boardFenSb;
    }

    /**
     * Decodes the Chess board from a FEN string
     * @param fenRows FEN string rows (ie. ["rnbqkbnr", "pppppppp", "8", ...])
     * @return Chess board
     */
    private static ChessPiece[] decodeBoard(String[] fenRows) {
        if (fenRows.length != 8) {
            throw new IllegalArgumentException("FEN string must have 8 rows");
        }

        ChessPiece[] board = new ChessPiece[64];
        // start at a8
        int currentBoardIndex = 56;

        // control variable to determine if we reached invalid fen due to not placing something
        int skippedOrPlaced = 0;
        // control variable to record last symbol of fen
        Character lastSymbol = null;

        // move forwards in the row
        for (String fenRow : fenRows) {

            // for each char in row
            for (char c : fenRow.toCharArray()) {

                // if we already have placed or skipped 8 pieces, and fen row is not over, then it's overflowing!
                if (skippedOrPlaced >= 8) {
                    throw new IllegalArgumentException("Invalid overflowing FEN string: " + fenRows[0]);
                }

                boolean isDigit = Character.isDigit(c);

                if (lastSymbol != null) {
                    // if last symbol was digit, then this is invalid
                    if (isDigit && Character.isDigit(lastSymbol)) {
                        throw new IllegalArgumentException("Invalid FEN string: " + fenRows[0]);
                    }
                }

                // if digit, then skip forward by that many spaces
                if (isDigit) {
                    int indicesToSkip = c - '0';
                    currentBoardIndex += indicesToSkip;
                    skippedOrPlaced += indicesToSkip;
                }
                // if symbol, then place piece at current position
                else {
                    board[currentBoardIndex++] = symbolToPiece(c);
                    skippedOrPlaced++;
                }

                // save last symbol for error checking
                lastSymbol = c;
            }

            // if we didn't skip and place 8 pieces per row, then the fen is invalid
            if (skippedOrPlaced != 8) {
                throw new IllegalArgumentException("Invalid FEN string: " + fenRows[0]);
            }

            // skip to the next row
            currentBoardIndex -= 16;
            // reset control variables
            lastSymbol = null;
            skippedOrPlaced = 0;
        }

        return board;
    }

    /**
     * Converts a symbol to a piece
     * @param symbol symbol to convert
     * @return piece corresponding to symbol
     */
    private static ChessPiece symbolToPiece(char symbol) {
        // determine piece class based on symbol
        Class<? extends ChessPiece> pieceClass = null;
        switch (Character.toUpperCase(symbol)) {
            case 'P': pieceClass = PawnPiece.class; break;
            case 'N': pieceClass = KnightPiece.class; break;
            case 'B': pieceClass = BishopPiece.class; break;
            case 'R': pieceClass = RookPiece.class; break;
            case 'Q': pieceClass = QueenPiece.class; break;
            case 'K': pieceClass = KingPiece.class; break;
            default: throw new IllegalArgumentException("Invalid symbol: " + symbol);
        }

        // instantiate piece with isWhite flag
        try {
            return pieceClass
                    .getConstructor(boolean.class)
                    .newInstance(Character.isUpperCase(symbol));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
