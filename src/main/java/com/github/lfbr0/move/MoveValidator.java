package com.github.lfbr0.move;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/1/26
 * @project regis
 */

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.Piece;
import com.github.lfbr0.board.piece.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generates legal moves for a given position and verifies if a move is legal
 */
public class MoveValidator {

    private MoveValidator() {
        // prevent instantiation
    }

    /**
     * Returns all legal moves for current side
     * @param state state of chess game
     * @return set of all legal moves
     */
    public static Set<Move> getLegalMoves(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        return IntStream
                .range(0, 64) // for all indices, check all pieces
                .filter(idx -> {
                    // check that we are grabbing a piece and it's our color
                    Piece piece = state.getPieceAtIndex(idx);
                    return piece != null && piece.isWhite() == state.isWhiteTurn();
                })
                .mapToObj(idx -> new Object[]{ idx, state.getPieceAtIndex(idx) }) // idx, piece pair
                .flatMap(idxPiecePair -> { // convert pair to chess move
                    int idx = (int) idxPiecePair[0];
                    Piece piece = (Piece) idxPiecePair[1];

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
                                            .map(type -> new Move(idx, to, type));
                                }

                                return Stream.of(new Move(idx, to, null));
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
    public static boolean isLegalMove(GameState state, Move move) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        } else if (move == null) {
            throw new IllegalArgumentException("Move cannot be null");
        }

        // is it inside the board?
        if (move.getFrom() < 0 || move.getFrom() >= 64) {
            return false;
        } else if (move.getTo() < 0 || move.getTo() >= 64) {
            return false;
        }

        // are we moving nowhere?
        if (move.getFrom() == move.getTo()) {
            return false;
        }

        Piece movingPiece = state.getPieceAtIndex(move.getFrom());
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
        Piece targetPiece = state.getPieceAtIndex(move.getTo());
        if (targetPiece != null && targetPiece.isWhite() == state.isWhiteTurn()) {
            return false;
        }

        // are we trying to capture a king?
        if (targetPiece instanceof KingPiece) {
            return false;
        }

        // are we promoting despite not being a pawn?
        if (move.getPromotionPieceClass().isPresent() && !(movingPiece instanceof PawnPiece)) {
            return false;
        }

        // check file/rank differences. An allowed offset alone can wrap across board edges.
        if (!validGeometry(movingPiece, move.getFrom(), move.getTo())) {
            return false;
        }

        // verify en passant, etc.
        if (movingPiece instanceof PawnPiece && !validPawnMove(state, move)) {
            return false;
        }

        // pawns, bishops, rooks, and queens cannot jump over pieces. Knights can. - path is clear?
        if (!isPathClear(state, move)) {
            return false;
        }

        // requires the correct right, king and rook on their starting squares, and an empty path.
        // the king cannot start in, pass through, or land in check.
        boolean castling = movingPiece instanceof KingPiece && Math.abs(move.getTo() - move.getFrom()) == 2;
        if (castling && !validCastling(state, move)) {
            return false;
        }

        // simulate move, then reject it if king is checked - board is copied to not alter state
        boolean movingSideIsWhite = state.isWhiteTurn();
        GameState movedState = MoveHandler.applyMove(state, move);

        int kingIndex = findKing(movedState, movingSideIsWhite);

        if (kingIndex < 0 || isSquareAttacked(movedState, kingIndex, !movingSideIsWhite)) {
            return false;
        }

        return true;
    }

    /**
     * Checks whether the side to move is in check.
     * @param state current game state
     * @return true if the current side's king is attacked
     */
    public static boolean isInCheck(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        boolean isWhite = state.isWhiteTurn();
        int kingIndex = findKing(state, isWhite);

        // a missing king means the position is invalid
        if (kingIndex < 0) {
            throw new IllegalArgumentException("Side to move has no king");
        }

        // is our king attacked by the opponent?
        return isSquareAttacked(state, kingIndex, !isWhite);
    }

    /**
     * UTILITIES - Auxiliary methods
     */


    /**
     * This checks if the move is valid for the piece and has the right geometry.
     * For example, h1 and a2 are one offset away, you cannot wrap around the board.
     * @param movingPiece moving piece
     * @param from starting square
     * @param to ending square
     * @return true if move has valid geometry
     */
    private static boolean validGeometry(Piece movingPiece, int from, int to) {
        int offset = to - from;

        // rule to remember: idx % 8 = file
        // rule to remember: idx / 8 = rank because 0/8 = 0, 8/8 = 1, 16/8 = 2, etc.

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

    /**
     * Pawn rules:
     * - Forward destination must be empty.
     * - Double advance requires the starting rank and both squares empty.
     * - Diagonal movement requires an enemy piece or valid en passant.
     * - En passant must remove the adjacent enemy pawn when simulating.
     * - Are we promoting if on last rank?
     * @param move move to perform
     * @param state current chess game state
     * @return true if move is valid for pawn
     */
    private static boolean validPawnMove(GameState state, Move move) {
        int from = move.getFrom();
        int to = move.getTo();
        Piece targetPiece = state.getPieceAtIndex(to);
        boolean isWhite = state.isWhiteTurn();

        // are we promoting before reaching the last rank?
        if (to / 8 != (isWhite ? 7 : 0)
                && move.getPromotionPieceClass().isPresent()) {
            return false;
        }

        // Are we promoting if on last rank?
        if (to / 8 == (isWhite ? 7 : 0) && move.getPromotionPieceClass().isEmpty()) {
            return false;
        }

        // are moving forward? file is same, rank is different (idx%8=file, idx/8=rank)
        if (from % 8 == to % 8 && from / 8 != to / 8) {
            // if moving forward one place, cannot have piece there
            if (targetPiece != null) {
                return false;
            }

            // if we're moving two places, we must be in second rows
            if (Math.abs(to - from) == 16) {
                // if white cannot have piece in middle and not doing this from second rank
                if (isWhite && (from / 8 != 1 || state.getPieceAtIndex(to-8) != null)) {
                    return false;
                }
                // if black, cannot have piece in middle and not doing this from first rank
                if (!isWhite && (from / 8 != 6 || state.getPieceAtIndex(to+8) != null)) {
                    return false;
                }
            }
        }
        // ok, so we're moving diagonally
        else {
            // diagonal movement requires an enemy piece or valid en passant
            if (targetPiece != null) {
                // ordinary capture: target must belong to the opponent
                if (targetPiece.isWhite() == isWhite) {
                    return false;
                }
            } else {
                // empty destination: it must match the current en passant target
                String enPassantSquare = state.getEnPassantSquare();

                if (enPassantSquare == null
                        || !enPassantSquare.equals(Move.indexToSquareString(to))) {
                    return false;
                }

                // white captures en passant from rank 5, black from rank 4
                int requiredRank = isWhite ? 4 : 3;
                if (from / 8 != requiredRank) {
                    return false;
                }

                // the captured pawn is behind the destination square
                int capturedPawnIndex = isWhite ? to - 8 : to + 8;
                Piece capturedPiece = state.getPieceAtIndex(capturedPawnIndex);

                // there must actually be an enemy pawn there
                if (!(capturedPiece instanceof PawnPiece) || capturedPiece.isWhite() == isWhite) {
                    return false;
                }
            }
        }

        // no issues found
        return true;
    }

    /**
     * Is the path between the two squares clear?
     * Bishops, rooks, and queens cannot jump over pieces. Knights can.
     * @param state current chess game state
     * @param move move to check
     * @return true if path is clear
     */
    private static boolean isPathClear(GameState state, Move move) {
        int from = move.getFrom();
        int to = move.getTo();
        Piece movingPiece = state.getPieceAtIndex(from);

        if (movingPiece instanceof BishopPiece) {
            boolean movingUp = to / 8 > from / 8;
            boolean movingRight = to % 8 > from % 8;

            // determine the index change for one diagonal square
            int step;

            if (movingUp && movingRight) {
                step = 9;
            } else if (movingUp) {
                step = 7;
            } else if (movingRight) {
                step = -7;
            } else {
                step = -9;
            }

            // check only squares between origin and destination
            for (int curr = from + step; curr != to; curr += step) {
                if (state.getPieceAtIndex(curr) != null) {
                    return false;
                }
            }
        }
        else if (movingPiece instanceof RookPiece) {
            boolean sameRank = from / 8 == to / 8;

            // determine the index change for one horizontal or vertical square
            int step;

            if (sameRank) {
                boolean movingRight = to % 8 > from % 8;
                step = movingRight ? 1 : -1;
            } else {
                boolean movingUp = to / 8 > from / 8;
                step = movingUp ? 8 : -8;
            }

            // check only squares between origin and destination
            for (int curr = from + step; curr != to; curr += step) {
                if (state.getPieceAtIndex(curr) != null) {
                    return false;
                }
            }
        }
        else if (movingPiece instanceof QueenPiece) {
            boolean sameRank = from / 8 == to / 8;
            boolean sameFile = from % 8 == to % 8;
            boolean movingUp = to / 8 > from / 8;
            boolean movingRight = to % 8 > from % 8;

            // determine the index change for one square
            int step;

            // moving horizontally, like a rook
            if (sameRank) {
                step = movingRight ? 1 : -1;
            }
            // moving vertically, like a rook
            else if (sameFile) {
                step = movingUp ? 8 : -8;
            }
            // otherwise moving diagonally, like a bishop
            else if (movingUp && movingRight) {
                step = 9;
            } else if (movingUp) {
                step = 7;
            } else if (movingRight) {
                step = -7;
            } else {
                step = -9;
            }

            // check only squares between origin and destination
            for (int curr = from + step; curr != to; curr += step) {
                if (state.getPieceAtIndex(curr) != null) {
                    return false;
                }
            }
        }

        // no issues found, path is clear
        return true;
    }

    /**
     * Checks castling rights, rook presence, empty path, and king safety.
     * @param state current game state
     * @param move candidate castling move
     * @return true if castling is allowed
     */
    private static boolean validCastling(GameState state, Move move) {
        boolean isWhite = state.isWhiteTurn();
        int from = move.getFrom();
        int to = move.getTo();

        // king must start at e1 or e8
        int kingStart = isWhite ? 4 : 60;

        if (from != kingStart) {
            return false;
        }

        // king must move two files on the same rank
        if (from / 8 != to / 8 || Math.abs(to - from) != 2) {
            return false;
        }

        boolean kingside = to > from;

        // do we still have the right to castle in this direction?
        boolean castlingAllowed;

        if (isWhite) {
            castlingAllowed = kingside
                    ? state.isWhiteKingsideCastlingAllowed()
                    : state.isWhiteQueensideCastlingAllowed();
        } else {
            castlingAllowed = kingside
                    ? state.isBlackKingsideCastlingAllowed()
                    : state.isBlackQueensideCastlingAllowed();
        }

        if (!castlingAllowed) {
            return false;
        }

        // the rook must be on its starting square and belong to us
        int rookIndex = kingStart + (kingside ? 3 : -4);
        Piece rook = state.getPieceAtIndex(rookIndex);

        if (!(rook instanceof RookPiece) || rook.isWhite() != isWhite) {
            return false;
        }

        // all squares between king and rook must be empty
        int step = kingside ? 1 : -1;

        for (int curr = from + step; curr != rookIndex; curr += step) {
            if (state.getPieceAtIndex(curr) != null) {
                return false;
            }
        }

        // cannot castle while already in check
        if (isSquareAttacked(state, from, !isWhite)) {
            return false;
        }

        // move king one square to check the square it passes through
        // removing it from its origin ensures it cannot hide an attack
        int transitIndex = from + step;
        GameState transitState = MoveHandler.applyMove(state, new Move(from, transitIndex));

        if (isSquareAttacked(transitState, transitIndex, !isWhite)) {
            return false;
        }

        // destination safety is checked by the final simulation in isLegalMove
        return true;
    }

    /**
     * Finds the king belonging to the requested side.
     * @param state game state to inspect
     * @param isWhite color of the king
     * @return king index, or -1 if missing
     */
    private static int findKing(GameState state, boolean isWhite) {
        for (int idx = 0; idx < 64; idx++) {
            Piece piece = state.getPieceAtIndex(idx);

            if (piece instanceof KingPiece && piece.isWhite() == isWhite) {
                return idx;
            }
        }

        return -1;
    }

    /**
     * Checks whether a square is attacked by the requested side.
     * Does not check whether attacking would expose the attacker's king.
     * @param state game state to inspect
     * @param target square being checked
     * @param attackingSideIsWhite color of the attacking side
     * @return true if the square is attacked
     */
    private static boolean isSquareAttacked(GameState state, int target, boolean attackingSideIsWhite) {

        for (int from = 0; from < 64; from++) {
            Piece piece = state.getPieceAtIndex(from);

            // only check pieces belonging to the attacking side
            if (piece == null || piece.isWhite() != attackingSideIsWhite) {
                continue;
            }

            int fileDelta = target % 8 - from % 8;
            int rankDelta = target / 8 - from / 8;
            int filesMoved = Math.abs(fileDelta);
            int ranksMoved = Math.abs(rankDelta);

            // a piece does not attack its own square
            if (filesMoved == 0 && ranksMoved == 0) {
                continue;
            }

            if (piece instanceof PawnPiece) {
                // pawns attack one square diagonally forward, not straight ahead
                int direction = attackingSideIsWhite ? 1 : -1;

                if (filesMoved == 1 && rankDelta == direction) {
                    return true;
                }
            } else if (piece instanceof KnightPiece) {
                // knights can jump over other pieces
                boolean knightAttack = (filesMoved == 1 && ranksMoved == 2) || (filesMoved == 2 && ranksMoved == 1);

                if (knightAttack) {
                    return true;
                }
            } else if (piece instanceof KingPiece) {
                // kings attack adjacent squares only; castling is not an attack
                if (filesMoved <= 1 && ranksMoved <= 1) {
                    return true;
                }
            } else {
                boolean straight = filesMoved == 0 || ranksMoved == 0;
                boolean diagonal = filesMoved == ranksMoved;

                boolean correctDirection = false;

                if (piece instanceof RookPiece) {
                    correctDirection = straight;
                } else if (piece instanceof BishopPiece) {
                    correctDirection = diagonal;
                } else if (piece instanceof QueenPiece) {
                    correctDirection = straight || diagonal;
                }

                // sliding attacks must have no pieces between attacker and target
                // isPathClear excludes the destination, which is what we need here
                if (correctDirection && isPathClear(state, new Move(from, target))) {
                    return true;
                }
            }
        }

        return false;
    }
}
