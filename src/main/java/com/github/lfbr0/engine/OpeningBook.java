package com.github.lfbr0.engine;

import com.github.lfbr0.board.GameState;
import com.github.lfbr0.board.piece.*;
import com.github.lfbr0.move.Move;
import com.github.lfbr0.move.MoveValidator;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/12/26
 * @project regis
 *
 * Good source: https://github.com/ChrisWhittington/polyglot-books
 */
public class OpeningBook {

    private static final String OPENINGS_FILENAME = "openings.bin";

    // each position can have several moves with different weights
    private final Map<Long, List<BookEntry>> bookEntries = new HashMap<>();

    // keep the encoded move until we know the position it belongs to
    private record BookEntry(int encodedMove, int weight) {
    }

    public OpeningBook() {
        loadBookEntriesFromPolyglotFile(OPENINGS_FILENAME);
    }

    /**
     * Load opening books from Polyglot openings bin and populate map.
     * @param filename name of file
     */
    private void loadBookEntriesFromPolyglotFile(String filename) {
        try (InputStream resource = OpeningBook.class.getResourceAsStream("/" + filename)) {
            if (resource == null) {
                throw new IllegalStateException("Missing opening book resource: " + filename);
            }

            DataInputStream input = new DataInputStream(new BufferedInputStream(resource));

            while (true) {
                // read the first byte separately to distinguish EOF from a truncated entry
                int firstByte = input.read();
                if (firstByte == -1) {
                    break;
                }

                // Polyglot stores all fields in big-endian order
                long hash = (long) firstByte << 56;
                for (int shift = 48; shift >= 0; shift -= 8) {
                    hash |= (long) input.readUnsignedByte() << shift;
                }

                int encodedMove = input.readUnsignedShort();
                int weight = input.readUnsignedShort();

                // learning data isn't needed for selecting moves
                input.readInt();

                // zero-weight entries are excluded from book selection
                if (weight == 0) {
                    continue;
                }

                bookEntries
                        .computeIfAbsent(hash, ignored -> new ArrayList<>())
                        .add(new BookEntry(encodedMove, weight));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load opening book: " + filename, e);
        }
    }

    /**
     * Find move according to opening books.
     * @param state game state
     * @param hash Polyglot zobrist hash of state
     * @return opening book move or empty if out of book
     */
    public Optional<Move> findOpeningBookMove(GameState state, long hash) {
        List<BookEntry> entries = bookEntries.get(hash);

        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        List<Move> legalMoves = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        long totalWeight = 0;

        for (BookEntry entry : entries) {
            Move move = decodeMove(state, entry.encodedMove());

            // ignore malformed entries and moves that aren't legal in this position
            if (move == null || !MoveValidator.isLegalMove(state, move)) {
                continue;
            }

            legalMoves.add(move);
            weights.add(entry.weight());
            totalWeight += entry.weight();
        }

        if (legalMoves.isEmpty()) {
            return Optional.empty();
        }

        // larger weights give moves a proportionally larger chance of selection
        long selection = ThreadLocalRandom.current().nextLong(totalWeight);

        for (int i = 0; i < legalMoves.size(); i++) {
            selection -= weights.get(i);

            if (selection < 0) {
                return Optional.of(legalMoves.get(i));
            }
        }

        throw new IllegalStateException("Opening book selection failed");
    }

    /**
     * Converts an encoded Polyglot move to our move representation.
     * @param state game state
     * @param encodedMove encoded book move
     * @return decoded move, or null if the encoding is invalid
     */
    private Move decodeMove(GameState state, int encodedMove) {
        // the highest bit is unused in the Polyglot move format
        if ((encodedMove & 0x8000) != 0) {
            return null;
        }

        int to = encodedMove & 0x3F;
        int from = (encodedMove >>> 6) & 0x3F;
        int promotion = (encodedMove >>> 12) & 0x7;

        // Polyglot encodes castling as the king moving to its rook's square
        if (state.getPieceAtIndex(from) instanceof KingPiece) {
            if (from == 4 && to == 7) {
                to = 6;
            } else if (from == 4 && to == 0) {
                to = 2;
            } else if (from == 60 && to == 63) {
                to = 62;
            } else if (from == 60 && to == 56) {
                to = 58;
            }
        }

        // promotion codes: 0=none, 1=knight, 2=bishop, 3=rook, 4=queen
        return switch (promotion) {
            case 0 -> new Move(from, to, null);
            case 1 -> new Move(from, to, KnightPiece.class);
            case 2 -> new Move(from, to, BishopPiece.class);
            case 3 -> new Move(from, to, RookPiece.class);
            case 4 -> new Move(from, to, QueenPiece.class);
            default -> null;
        };
    }
}