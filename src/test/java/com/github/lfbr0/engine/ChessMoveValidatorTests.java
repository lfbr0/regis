package com.github.lfbr0.engine;

/**
 * @author Luís Filipe Ribeiro (lfbr0 @ Github)
 * @date 11/1/26
 * @project regis
 */

import static org.junit.Assert.*;

import com.github.lfbr0.board.ChessGameState;
import com.github.lfbr0.board.ChessPiece;
import com.github.lfbr0.board.FenCodec;
import com.github.lfbr0.board.piece.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Contract: public static validator methods, standard chess, syntax-valid moves.
 * Null arguments throw IllegalArgumentException.
 * Draw counters do not suppress otherwise legal moves.
 * Positions contain both kings; reachability validation is outside this suite.
 */
public class ChessMoveValidatorTests {

    @Test
    public void whitePawnCanAdvanceOneOrTwoSquaresInitially() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pd2");
        assertMovesFrom(state, "d2", "d2d3", "d2d4");
    }

    @Test
    public void blackPawnCanAdvanceOneOrTwoSquaresInitially() {
        ChessGameState state = position(false, "-", null, "Ka1", "kh8", "pd7");
        assertMovesFrom(state, "d7", "d7d6", "d7d5");
    }

    @Test
    public void whitePawnCannotDoubleAdvanceAfterLeavingStartingRank() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pd3");
        assertMovesFrom(state, "d3", "d3d4");
    }

    @Test
    public void blackPawnCannotDoubleAdvanceAfterLeavingStartingRank() {
        ChessGameState state = position(false, "-", null, "Ka1", "kh8", "pd6");
        assertMovesFrom(state, "d6", "d6d5");
    }

    @Test
    public void whitePawnCannotJumpOverFriendlyPiece() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pd2", "Nd3");
        assertMovesFrom(state, "d2");
    }

    @Test
    public void whitePawnCannotJumpOverEnemyPiece() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pd2", "nd3");
        assertMovesFrom(state, "d2");
    }

    @Test
    public void blackPawnCannotJumpOverPiece() {
        ChessGameState state = position(false, "-", null, "Ka1", "kh8", "pd7", "nd6");
        assertMovesFrom(state, "d7");
    }

    @Test
    public void whitePawnCanAdvanceOneWhenDoubleDestinationBlocked() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pd2", "nd4");
        assertMovesFrom(state, "d2", "d2d3");
    }

    @Test
    public void blackPawnCanAdvanceOneWhenDoubleDestinationBlocked() {
        ChessGameState state = position(false, "-", null, "Ka1", "kh8", "pd7", "Nd5");
        assertMovesFrom(state, "d7", "d7d6");
    }

    @Test
    public void whitePawnCapturesBothDiagonals() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pd4", "nc5", "be5");
        assertMovesFrom(state, "d4", "d4d5", "d4c5", "d4e5");
    }

    @Test
    public void blackPawnCapturesBothDiagonals() {
        ChessGameState state = position(false, "-", null, "Ka1", "kh8", "pd5", "Nc4", "Be4");
        assertMovesFrom(state, "d5", "d5d4", "d5c4", "d5e4");
    }

    @Test
    public void pawnCannotCaptureForward() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pd4", "nd5");
        assertMovesFrom(state, "d4");
    }

    @Test
    public void pawnCannotCaptureFriendlyPieces() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pd4", "Nc5", "Be5");
        assertMovesFrom(state, "d4", "d4d5");
    }

    @Test
    public void aFilePawnDoesNotWrapAroundBoard() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pa4", "nh4", "nb5");
        assertMovesFrom(state, "a4", "a4a5", "a4b5");
    }

    @Test
    public void hFilePawnDoesNotWrapAroundBoard() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Ph4", "na5", "ng5");
        assertMovesFrom(state, "h4", "h4h5", "h4g5");
    }

    @Test
    public void knightHasEightMovesInCenter() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Nd4");
        assertMovesFrom(state, "d4", "d4b3", "d4b5", "d4c2", "d4c6", "d4e2", "d4e6", "d4f3", "d4f5");
    }

    @Test
    public void knightHasTwoMovesInCorner() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "Na1");
        assertMovesFrom(state, "a1", "a1b3", "a1c2");
    }

    @Test
    public void knightCanJumpOverPieces() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Nd4", "Pc4", "Pe4", "Pd3", "Pd5");
        assertMovesFrom(state, "d4", "d4b3", "d4b5", "d4c2", "d4c6", "d4e2", "d4e6", "d4f3", "d4f5");
    }

    @Test
    public void knightCannotLandOnFriendlyPiece() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Nd4", "Pb3");
        assertMovesFrom(state, "d4", "d4b5", "d4c2", "d4c6", "d4e2", "d4e6", "d4f3", "d4f5");
    }

    @Test
    public void knightCanCaptureEnemy() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Nd4", "pb3");
        assertMove(state, "d4b3", true);
    }

    @Test
    public void rookMovesAlongEveryUnblockedRankAndFile() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Rd4");
        assertMovesFrom(state, "d4", "d4a4", "d4b4", "d4c4", "d4e4", "d4f4", "d4g4", "d4h4", "d4d1", "d4d2", "d4d3", "d4d5", "d4d6", "d4d7", "d4d8");
    }

    @Test
    public void bishopMovesAlongEveryUnblockedDiagonal() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Bd4");
        assertMovesFrom(state, "d4", "d4a1", "d4b2", "d4c3", "d4e5", "d4f6", "d4g7", "d4h8", "d4a7", "d4b6", "d4c5", "d4e3", "d4f2", "d4g1");
    }

    @Test
    public void queenCombinesRookAndBishopMoves() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Qd4");
        assertMovesFrom(state, "d4", "d4a4", "d4b4", "d4c4", "d4e4", "d4f4", "d4g4", "d4h4", "d4d1", "d4d2", "d4d3", "d4d5", "d4d6", "d4d7", "d4d8", "d4a1", "d4b2", "d4c3", "d4e5", "d4f6", "d4g7", "d4h8", "d4a7", "d4b6", "d4c5", "d4e3", "d4f2", "d4g1");
    }

    @Test
    public void rookCannotMoveDiagonally() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Rd4");
        assertMove(state, "d4e5", false);
    }

    @Test
    public void bishopCannotMoveOrthogonally() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Bd4");
        assertMove(state, "d4d5", false);
    }

    @Test
    public void queenCannotMoveLikeKnight() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Qd4");
        assertMove(state, "d4f5", false);
    }

    @Test
    public void rookCannotCaptureFriendlyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Rd4", "Pd6");
        assertMove(state, "d4d6", false);
    }

    @Test
    public void rookCannotJumpFriendlyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Rd4", "Pd6");
        assertMove(state, "d4d7", false);
    }

    @Test
    public void rookCanCaptureEnemyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Rd4", "pd6");
        assertMove(state, "d4d6", true);
    }

    @Test
    public void rookCannotJumpEnemyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Rd4", "pd6");
        assertMove(state, "d4d7", false);
    }

    @Test
    public void bishopCannotCaptureFriendlyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Bd4", "Pf6");
        assertMove(state, "d4f6", false);
    }

    @Test
    public void bishopCannotJumpFriendlyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Bd4", "Pf6");
        assertMove(state, "d4g7", false);
    }

    @Test
    public void bishopCanCaptureEnemyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Bd4", "pf6");
        assertMove(state, "d4f6", true);
    }

    @Test
    public void bishopCannotJumpEnemyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Bd4", "pf6");
        assertMove(state, "d4g7", false);
    }

    @Test
    public void queenCannotCaptureFriendlyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Qd4", "Pf6");
        assertMove(state, "d4f6", false);
    }

    @Test
    public void queenCannotJumpFriendlyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Qd4", "Pf6");
        assertMove(state, "d4g7", false);
    }

    @Test
    public void queenCanCaptureEnemyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Qd4", "pf6");
        assertMove(state, "d4f6", true);
    }

    @Test
    public void queenCannotJumpEnemyBlocker() {
        ChessGameState state = position(true, "-", null, "Ka2", "kh7", "Qd4", "pf6");
        assertMove(state, "d4g7", false);
    }

    @Test
    public void kingHasEightMovesInCenter() {
        ChessGameState state = position(true, "-", null, "Kd4", "kh8");
        assertMovesFrom(state, "d4", "d4c3", "d4d3", "d4e3", "d4c4", "d4e4", "d4c5", "d4d5", "d4e5");
    }

    @Test
    public void kingHasThreeMovesInCorner() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8");
        assertMovesFrom(state, "a1", "a1a2", "a1b1", "a1b2");
    }

    @Test
    public void kingCannotLandOnFriendlyPiece() {
        ChessGameState state = position(true, "-", null, "Kd4", "kh8", "Pc4");
        assertMove(state, "d4c4", false);
    }

    @Test
    public void kingCanCaptureUndefendedEnemy() {
        ChessGameState state = position(true, "-", null, "Kd4", "kh8", "nc4");
        assertMove(state, "d4c4", true);
    }

    @Test
    public void kingCannotApproachEnemyKingOne3() {
        ChessGameState state = position(true, "-", null, "Kd4", "kf4");
        assertMove(state, "d4e3", false);
    }

    @Test
    public void kingCannotApproachEnemyKingOne4() {
        ChessGameState state = position(true, "-", null, "Kd4", "kf4");
        assertMove(state, "d4e4", false);
    }

    @Test
    public void kingCannotApproachEnemyKingOne5() {
        ChessGameState state = position(true, "-", null, "Kd4", "kf4");
        assertMove(state, "d4e5", false);
    }

    @Test
    public void kingCannotEnterRookAttack() {
        ChessGameState state = position(true, "-", null, "Ke1", "ka8", "re8");
        assertMove(state, "e1e2", false);
    }

    @Test
    public void kingCannotEnterBishopAttack() {
        ChessGameState state = position(true, "-", null, "Ke1", "ka8", "bb4");
        assertMove(state, "e1d2", false);
    }

    @Test
    public void kingCannotEnterKnightAttack() {
        ChessGameState state = position(true, "-", null, "Ke1", "ka8", "nf4");
        assertMove(state, "e1e2", false);
    }

    @Test
    public void kingCannotEnterBlackPawnAttack() {
        ChessGameState state = position(true, "-", null, "Ke3", "ka8", "pd5");
        assertMove(state, "e3e4", false);
    }

    @Test
    public void kingCannotCaptureDefendedPiece() {
        ChessGameState state = position(true, "-", null, "Ke1", "ka8", "re8", "ne2");
        assertMove(state, "e1e2", false);
    }

    @Test
    public void kingCanStandInFrontOfEnemyPawn() {
        ChessGameState state = position(true, "-", null, "Ke3", "ka8", "pe5");
        assertMove(state, "e3e4", true);
    }

    @Test
    public void pinnedEnemyPieceStillAttacksKingDestination() {
        ChessGameState state = position(true, "-", null, "Kc4", "Re1", "ke8", "ne7");
        assertMove(state, "c4d5", false);
    }

    @Test
    public void pinnedKnightCannotMove() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "re8", "Ne2");
        assertMovesFrom(state, "e2");
    }

    @Test
    public void pinnedRookCannotExposeKing() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "re8", "Re2");
        assertMove(state, "e2d2", false);
    }

    @Test
    public void pinnedRookCanStayOnPinLine() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "re8", "Re2");
        assertMove(state, "e2e3", true);
    }

    @Test
    public void pinnedRookCanCapturePinner() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "re8", "Re2");
        assertMove(state, "e2e8", true);
    }

    @Test
    public void pinnedPawnCanAdvanceAlongPinLine() {
        ChessGameState state = position(true, "-", null, "Kd1", "kh8", "rd8", "Pd2");
        assertMove(state, "d2d4", true);
    }

    @Test
    public void pinnedPawnCannotCaptureOffPinLine() {
        ChessGameState state = position(true, "-", null, "Kd1", "kh8", "rd8", "Pd2", "nc3");
        assertMove(state, "d2c3", false);
    }

    @Test
    public void unrelatedMoveCannotIgnoreCheck() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "re8", "Na1");
        assertMove(state, "a1b3", false);
    }

    @Test
    public void pieceCanBlockCheck() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "re8", "Bd2");
        assertMove(state, "d2e3", true);
    }

    @Test
    public void pieceCanCaptureCheckingPiece() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "re8", "Ba4");
        assertMove(state, "a4e8", true);
    }

    @Test
    public void doubleCheckAllowsOnlyKingMoves() {
        ChessGameState state = position(true, "-", null, "Ke1", "kh8", "re8", "bb4", "Ra1");
        Set<ChessMove> moves = ChessMoveValidator.getLegalMoves(state);
        assertFalse(moves.isEmpty());
        for (ChessMove move : moves) {
            assertEquals("e1", move.getFrom());
        }
    }

    @Test
    public void whiteKingCanCastleBothSides() {
        ChessGameState state = position(true, "KQ", null, "Ke1", "Ra1", "Rh1", "ke8");
        assertMovesFrom(state, "e1", "e1d1", "e1f1", "e1d2", "e1e2", "e1f2", "e1g1", "e1c1");
    }

    @Test
    public void blackKingCanCastleBothSides() {
        ChessGameState state = position(false, "kq", null, "Ke1", "ke8", "ra8", "rh8");
        assertMovesFrom(state, "e8", "e8d8", "e8f8", "e8d7", "e8e7", "e8f7", "e8g8", "e8c8");
    }

    @Test
    public void whiteCannotCastleWithoutWhiteRightsNone() {
        ChessGameState state = position(true, "-", null, "Ke1", "Ra1", "Rh1", "ke8");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void whiteCannotCastleWithoutWhiteRightsBlackOnly() {
        ChessGameState state = position(true, "kq", null, "Ke1", "Ra1", "Rh1", "ke8");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void whiteCannotCastleQueensideWithoutRight() {
        ChessGameState state = position(true, "K", null, "Ke1", "Ra1", "Rh1", "ke8");
        assertMove(state, "e1c1", false);
    }

    @Test
    public void whiteCannotCastleKingsideWithoutRight() {
        ChessGameState state = position(true, "Q", null, "Ke1", "Ra1", "Rh1", "ke8");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void cannotCastleWithoutRook() {
        ChessGameState state = position(true, "K", null, "Ke1", "ke8");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void cannotCastleWithEnemyRook() {
        ChessGameState state = position(true, "K", null, "Ke1", "ke8", "rh1");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void kingsideCastlingBlockedAtf1() {
        ChessGameState state = position(true, "K", null, "Ke1", "Rh1", "ke8", "Nf1");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void kingsideCastlingBlockedAtg1() {
        ChessGameState state = position(true, "K", null, "Ke1", "Rh1", "ke8", "Ng1");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void queensideCastlingBlockedAtb1() {
        ChessGameState state = position(true, "Q", null, "Ke1", "Ra1", "ke8", "Nb1");
        assertMove(state, "e1c1", false);
    }

    @Test
    public void queensideCastlingBlockedAtc1() {
        ChessGameState state = position(true, "Q", null, "Ke1", "Ra1", "ke8", "Nc1");
        assertMove(state, "e1c1", false);
    }

    @Test
    public void queensideCastlingBlockedAtd1() {
        ChessGameState state = position(true, "Q", null, "Ke1", "Ra1", "ke8", "Nd1");
        assertMove(state, "e1c1", false);
    }

    @Test
    public void cannotCastleWhileInCheck() {
        ChessGameState state = position(true, "K", null, "Ke1", "Rh1", "ka8", "re8");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void cannotCastleThroughCheck() {
        ChessGameState state = position(true, "K", null, "Ke1", "Rh1", "ka8", "rf8");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void cannotCastleIntoCheck() {
        ChessGameState state = position(true, "K", null, "Ke1", "Rh1", "ka8", "rg8");
        assertMove(state, "e1g1", false);
    }

    @Test
    public void queensideCannotCastleThroughCheck() {
        ChessGameState state = position(true, "Q", null, "Ke1", "Ra1", "kh8", "rd8");
        assertMove(state, "e1c1", false);
    }

    @Test
    public void queensideCannotCastleIntoCheck() {
        ChessGameState state = position(true, "Q", null, "Ke1", "Ra1", "kh8", "rc8");
        assertMove(state, "e1c1", false);
    }

    @Test
    public void queensideCastlingAllowedWhenOnlyBFileAttacked() {
        ChessGameState state = position(true, "Q", null, "Ke1", "Ra1", "kh8", "rb8");
        assertMove(state, "e1c1", true);
    }

    @Test
    public void castlingAllowedWhenRookIsAttacked() {
        ChessGameState state = position(true, "K", null, "Ke1", "Rh1", "ka8", "rh8");
        assertMove(state, "e1g1", true);
    }

    @Test
    public void blackCannotCastleThroughCheck() {
        ChessGameState state = position(true, "k", null, "Ka1", "Rf1", "ke8", "rh8");
        assertMove(state, "e8g8", false);
    }

    @Test
    public void whiteEnPassant() {
        ChessGameState state = position(true, "-", "d6", "Ka1", "kh8", "Pe5", "pd5");
        assertMovesFrom(state, "e5", "e5e6", "e5d6");
    }

    @Test
    public void blackEnPassant() {
        ChessGameState state = position(false, "-", "d3", "Ka1", "kh8", "pe4", "Pd4");
        assertMovesFrom(state, "e4", "e4e3", "e4d3");
    }

    @Test
    public void enPassantUnavailableWithoutTarget() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pe5", "pd5");
        assertMove(state, "e5d6", false);
    }

    @Test
    public void enPassantCannotUseDifferentTarget() {
        ChessGameState state = position(true, "-", "f6", "Ka1", "kh8", "Pe5", "pd5");
        assertMove(state, "e5d6", false);
    }

    @Test
    public void enPassantCannotExposeVerticalCheck() {
        ChessGameState state = position(true, "-", "d6", "Ke1", "kh8", "re8", "Pe5", "pd5");
        assertMove(state, "e5d6", false);
    }

    @Test
    public void enPassantCannotExposeHorizontalCheck() {
        ChessGameState state = position(true, "-", "f6", "Kh5", "ka8", "ra5", "Pg5", "pf5");
        assertMove(state, "g5f6", false);
    }

    @Test
    public void enPassantCanCaptureCheckingPawn() {
        ChessGameState state = position(true, "-", "d6", "Ke4", "kh8", "Pe5", "pd5");
        assertMove(state, "e5d6", true);
    }

    @Test
    public void whitePromotionOffersAllFourPieces() {
        ChessGameState state = position(true, "-", null, "Kh1", "kh8", "Pa7");
        assertMovesFrom(state, "a7", "a7a8q", "a7a8r", "a7a8b", "a7a8n");
    }

    @Test
    public void whitePromotionRequiresSuffix() {
        ChessGameState state = position(true, "-", null, "Kh1", "kh8", "Pa7");
        assertMove(state, "a7a8", false);
    }

    @Test
    public void blackPromotionOffersAllFourPieces() {
        ChessGameState state = position(false, "-", null, "Kh1", "kh8", "pa2");
        assertMovesFrom(state, "a2", "a2a1q", "a2a1r", "a2a1b", "a2a1n");
    }

    @Test
    public void blackPromotionRequiresSuffix() {
        ChessGameState state = position(false, "-", null, "Kh1", "kh8", "pa2");
        assertMove(state, "a2a1", false);
    }

    @Test
    public void capturePromotionOffersFourMoreChoices() {
        ChessGameState state = position(true, "-", null, "Kh1", "kh8", "Pa7", "rb8");
        assertMovesFrom(state, "a7", "a7a8q", "a7a8r", "a7a8b", "a7a8n", "a7b8q", "a7b8r", "a7b8b", "a7b8n");
    }

    @Test
    public void promotionSuffixForbiddenOnOrdinaryPawnMove() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Pe2");
        assertMove(state, "e2e4q", false);
    }

    @Test
    public void promotionSuffixForbiddenForKnight() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Nb1");
        assertMove(state, "b1c3q", false);
    }

    @Test
    public void cannotMoveOpponentPiece() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "pd7");
        assertMove(state, "d7d6", false);
    }

    @Test
    public void cannotMoveFromEmptySquare() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8");
        assertMove(state, "d4d5", false);
    }

    @Test
    public void cannotRemainOnSameSquare() {
        ChessGameState state = position(true, "-", null, "Ka1", "kh8", "Rd4");
        assertMove(state, "d4d4", false);
    }

    @Test
    public void startingPositionHasExactlyTwentyMoves() {
        ChessGameState state = new ChessGameState(FenCodec.DEFAULT_FEN_STRING);
        assertAllMoves(state,
                "a2a3", "a2a4", "b2b3", "b2b4", "c2c3", "c2c4",
                "d2d3", "d2d4", "e2e3", "e2e4", "f2f3", "f2f4",
                "g2g3", "g2g4", "h2h3", "h2h4",
                "b1a3", "b1c3", "g1f3", "g1h3");
    }

    @Test
    public void blackStartingPositionHasExactlyTwentyMoves() {
        ChessGameState state = new ChessGameState(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1");
        assertAllMoves(state,
                "a7a6", "a7a5", "b7b6", "b7b5", "c7c6", "c7c5",
                "d7d6", "d7d5", "e7e6", "e7e5", "f7f6", "f7f5",
                "g7g6", "g7g5", "h7h6", "h7h5",
                "b8a6", "b8c6", "g8f6", "g8h6");
    }

    @Test
    public void checkmateHasNoLegalMoves() {
        ChessGameState state = new ChessGameState("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1");
        assertAllMoves(state);
        assertMove(state, "h8h7", false);
    }

    @Test
    public void stalemateHasNoLegalMoves() {
        ChessGameState state = new ChessGameState("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
        assertAllMoves(state);
        assertMove(state, "h8h7", false);
    }

    @Test
    public void highHalfmoveClockDoesNotRemoveGeometricallyLegalMoves() {
        ChessGameState state = new ChessGameState("7k/8/8/8/8/8/8/K7 w - - 150 100");
        assertAllMoves(state, "a1a2", "a1b1", "a1b2");
    }

    @Test
    public void generationDoesNotMutateInputState() {
        ChessGameState state = new ChessGameState(FenCodec.DEFAULT_FEN_STRING);
        String before = FenCodec.encode(state);
        ChessPiece[] references = state.getBoard().clone();
        ChessMoveValidator.getLegalMoves(state);
        assertEquals(before, FenCodec.encode(state));
        for (int i = 0; i < 64; i++) {
            assertSame("Square " + i, references[i], state.getBoard()[i]);
        }
    }

    @Test
    public void validationDoesNotMutateInputState() {
        ChessGameState state = new ChessGameState(FenCodec.DEFAULT_FEN_STRING);
        String before = FenCodec.encode(state);
        assertMove(state, "e2e4", true);
        assertMove(state, "e2e5", false);
        assertEquals(before, FenCodec.encode(state));
    }

    @Test
    public void repeatedGenerationReturnsSameValues() {
        ChessGameState state = new ChessGameState(FenCodec.DEFAULT_FEN_STRING);
        assertEquals(ChessMoveValidator.getLegalMoves(state), ChessMoveValidator.getLegalMoves(state));
    }

    @Test
    public void generationRejectsNullState() {
        assertThrows(IllegalArgumentException.class, () -> ChessMoveValidator.getLegalMoves(null));
    }

    @Test
    public void validationRejectsNullState() {
        assertThrows(IllegalArgumentException.class, () -> ChessMoveValidator.isLegalMove(null, new ChessMove("e2e4")));
    }

    @Test
    public void validationRejectsNullMove() {
        assertThrows(IllegalArgumentException.class, () -> ChessMoveValidator.isLegalMove(new ChessGameState(FenCodec.DEFAULT_FEN_STRING), null));
    }


    @Test
    public void specialMoveGenerationDoesNotMutateState() {
        ChessGameState[] states = {
                position(true, "KQ", null, "Ke1", "Ra1", "Rh1", "ke8"),
                position(true, "-", "d6", "Ka1", "kh8", "Pe5", "pd5"),
                position(true, "-", null, "Kh1", "kh8", "Pa7")
        };
        for (ChessGameState state : states) {
            String before = FenCodec.encode(state);
            ChessPiece[] references = state.getBoard().clone();
            ChessMoveValidator.getLegalMoves(state);
            assertEquals(before, FenCodec.encode(state));
            for (int i = 0; i < 64; i++) {
                assertSame("Square " + i, references[i], state.getBoard()[i]);
            }
        }
    }

    @Test
    public void blackKnightHasEightMovesInCenter() {
        ChessGameState state = position(false, "-", null, "Ka2", "kh7", "nd4");
        assertMovesFrom(state, "d4",
                "d4b3", "d4b5", "d4c2", "d4c6",
                "d4e2", "d4e6", "d4f3", "d4f5");
    }

    @Test
    public void blackPawnCannotCaptureForward() {
        ChessGameState state = position(false, "-", null, "Ka1", "kh8", "pd5", "Nd4");
        assertMovesFrom(state, "d5");
    }

    @Test
    public void blackCannotMoveWhitePiece() {
        ChessGameState state = position(false, "-", null, "Ka1", "kh8", "Pe2");
        assertMove(state, "e2e4", false);
    }

    @Test
    public void blackPinnedKnightCannotMove() {
        ChessGameState state = position(false, "-", null, "Kh1", "ke8", "Re1", "ne7");
        assertMovesFrom(state, "e7");
    }

    @Test
    public void blackCannotCastleIntoCheck() {
        ChessGameState state = position(false, "k", null, "Ka1", "Rg1", "ke8", "rh8");
        assertMove(state, "e8g8", false);
    }

    @Test
    public void blackEnPassantCannotExposeKing() {
        ChessGameState state = position(false, "-", "d3",
                "Kh1", "ke8", "Re1", "pe4", "Pd4");
        assertMove(state, "e4d3", false);
    }

    @Test
    public void blackCapturePromotionOffersAllFourChoices() {
        ChessGameState state = position(false, "-", null, "Kh1", "kh8", "pa2", "Rb1");
        assertMovesFrom(state, "a2",
                "a2a1q", "a2a1r", "a2a1b", "a2a1n",
                "a2b1q", "a2b1r", "a2b1b", "a2b1n");
    }

    private static void assertMove(ChessGameState state, String uci, boolean expected) {
        ChessMove move = new ChessMove(uci);
        assertEquals("isMoveLegal: " + uci,
                expected, ChessMoveValidator.isLegalMove(state, move));
        Set<ChessMove> generated = ChessMoveValidator.getLegalMoves(state);
        assertNotNull("Generated moves", generated);
        assertEquals("Generated membership: " + uci, expected, generated.contains(move));
    }

    private static void assertMovesFrom(
            ChessGameState state, String from, String... expected) {
        Set<ChessMove> generated = ChessMoveValidator.getLegalMoves(state);
        assertNotNull("Generated moves", generated);
        Set<String> actual = generated.stream()
                .filter(move -> move.getFrom().equals(from))
                .map(ChessMove::getUciMoveString)
                .collect(Collectors.toSet());
        assertEquals("Moves from " + from, new HashSet<>(Arrays.asList(expected)), actual);

        // Check both APIs against every destination, including promotion suffixes.
        for (char file = 'a'; file <= 'h'; file++) {
            for (char rank = '1'; rank <= '8'; rank++) {
                for (String suffix : new String[] {"", "q", "r", "b", "n"}) {
                    String uci = from + file + rank + suffix;
                    assertEquals("isMoveLegal: " + uci,
                            actual.contains(uci),
                            ChessMoveValidator.isLegalMove(state, new ChessMove(uci)));
                }
            }
        }
    }

    private static void assertAllMoves(ChessGameState state, String... expected) {
        Set<ChessMove> generated = ChessMoveValidator.getLegalMoves(state);
        assertNotNull("Generated moves", generated);
        Set<String> actual = generated.stream()
                .map(ChessMove::getUciMoveString)
                .collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(expected)), actual);
        for (ChessMove move : generated) {
            assertTrue("Generated move must be legal: " + move.getUciMoveString(),
                    ChessMoveValidator.isLegalMove(state, move));
        }
    }

    /**
     * Entries are piece symbol followed by square, e.g. "Ke1", "pd5".
     * Direct construction keeps fixtures independent of the FEN parser.
     */
    private static ChessGameState position(
            boolean white, String rights, String ep, String... entries) {
        ChessPiece[] board = new ChessPiece[64];
        for (String entry : entries) {
            char symbol = entry.charAt(0);
            int index = (entry.charAt(2) - '1') * 8 + entry.charAt(1) - 'a';
            if (board[index] != null) {
                throw new IllegalArgumentException("Duplicate fixture square: " + entry);
            }
            boolean pieceWhite = Character.isUpperCase(symbol);
            board[index] = switch (Character.toUpperCase(symbol)) {
                case 'P' -> new PawnPiece(pieceWhite);
                case 'N' -> new KnightPiece(pieceWhite);
                case 'B' -> new BishopPiece(pieceWhite);
                case 'R' -> new RookPiece(pieceWhite);
                case 'Q' -> new QueenPiece(pieceWhite);
                case 'K' -> new KingPiece(pieceWhite);
                default -> throw new IllegalArgumentException("Fixture piece: " + entry);
            };
        }
        return new ChessGameState(
                board, white, rights.contains("K"), rights.contains("Q"),
                rights.contains("k"), rights.contains("q"), ep, 0, 1);
    }
}