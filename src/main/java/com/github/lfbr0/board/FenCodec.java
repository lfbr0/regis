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

    public static String encode(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("Chess game state cannot be null");
        }

        // castling rights
        StringBuilder castling = new StringBuilder();
        if (state.isWhiteKingsideCastling()) castling.append('K');
        if (state.isWhiteQueensideCastling()) castling.append('Q');
        if (state.isBlackKingsideCastling()) castling.append('k');
        if (state.isBlackQueensideCastling()) castling.append('q');

        // en passant square
        String enPassant = state.getEnPassantSquare();
        if (enPassant != null && !enPassant.matches("[a-h][36]")) {
            throw new IllegalArgumentException("Invalid en passant square: " + enPassant);
        }

        if (state.getHalfmoveClock() < 0) {
            throw new IllegalArgumentException("Halfmove clock cannot be negative");
        }

        if (state.getFullmoveNumber() < 1) {
            throw new IllegalArgumentException("Fullmove number must be at least 1");
        }

        return new StringBuilder()
                .append(encodeBoard(state.getBoardCopy())) // actual board encoding
                .append(' ').append(state.isWhiteTurn() ? 'w' : 'b')
                .append(' ').append(castling.isEmpty() ? "-" : castling.toString())
                .append(' ').append(enPassant == null ? "-" : enPassant)
                .append(' ').append(state.getHalfmoveClock())
                .append(' ').append(state.getFullmoveNumber())
                .toString();
    }

    public static GameState decode(String fen) {
        if (fen == null) {
            throw new IllegalArgumentException("FEN string cannot be null");
        }

        String[] fenTokens = fen.strip().split("\\s+");
        if (fenTokens.length != 1 && fenTokens.length != 2 && fenTokens.length != 6) {
            throw new IllegalArgumentException("Expected 1, 2, or 6 FEN fields");
        }

        // start by deciphering the rowboard
        String fenRowsString = fenTokens[0];
        if (!fenRowsString.matches(FEN_ROWS_REGEX)) {
            throw new IllegalArgumentException("Invalid FEN rows string: " + fenRowsString);
        }
        Piece[] board = decodeBoard(fenRowsString.split("/"));

        // then, the turn - if no token, assume white
        boolean isWhiteTurn = true;
        if (fenTokens.length >= 2) {
            String activeColor = fenTokens[1];
            // validate color
            isWhiteTurn = switch (activeColor) {
                case "w" -> true;
                case "b" -> false;
                default -> throw new IllegalArgumentException("Invalid active color: " + activeColor);
            };
        }

        // defaults for abbreviated input.
        String castling = "-";
        String enPassant = null;
        int halfmoveClock = 0;
        int fullmoveNumber = 1;

        // parse castling rights, etc
        if (fenTokens.length == 6) {
            castling = fenTokens[2];

            if (!castling.matches("-|KQ?k?q?|Qk?q?|kq?|q")) {
                throw new IllegalArgumentException("Invalid castling rights: " + castling);
            }

            if (!fenTokens[3].matches("-|[a-h][36]")) {
                throw new IllegalArgumentException("Invalid en passant square: " + fenTokens[3]);
            }

            enPassant = fenTokens[3].equals("-") ? null : fenTokens[3];
            halfmoveClock = parseCounter(fenTokens[4], 0, "halfmove clock");
            fullmoveNumber = parseCounter(fenTokens[5], 1, "fullmove number");
        }

        return new GameState(
                board,
                isWhiteTurn,
                castling.contains("K"),
                castling.contains("Q"),
                castling.contains("k"),
                castling.contains("q"),
                enPassant,
                halfmoveClock,
                fullmoveNumber
        );
    }

    /**
     * Utilities below
     */

    /**
     * Encodes the Chess board to a FEN string rows
     * @param board Chess board
     * @return FEN string rows
     */
    private static StringBuilder encodeBoard(Piece[] board) {
        if (board == null || board.length != 64) {
            throw new IllegalArgumentException("Invalid board");
        }

        StringBuilder boardFenSb = new StringBuilder(72);

        int currentBoardIndex = 56; // start at a8
        while (true) {
            // count empty spaces between pieces
            int emptySpaces = 0;

            for (int i = 0; i < 8; i++) {
                Piece piece = board[currentBoardIndex + i];

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
    private static Piece[] decodeBoard(String[] fenRows) {
        if (fenRows.length != 8) {
            throw new IllegalArgumentException("FEN string must have 8 rows");
        }

        Piece[] board = new Piece[64];
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
    private static Piece symbolToPiece(char symbol) {
        // determine piece class based on symbol
        Class<? extends Piece> pieceClass = null;
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

    /**
     * Parses a counter value from a string.
     * @param value String value to parse
     * @param minimum Minimum value allowed
     * @param field Name of the field being parsed
     * @return Parsed value
     */
    private static int parseCounter(String value, int minimum, String field) {
        if (!value.matches("[0-9]+")) {
            throw new IllegalArgumentException("Invalid " + field + ": " + value);
        }

        final int number;

        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " is too large: " + value, e);
        }

        if (number < minimum) {
            throw new IllegalArgumentException(
                    field + " must be at least " + minimum);
        }

        return number;
    }
}
