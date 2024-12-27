package Modular;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AI {
    public AIDifficulty difficulty;
    public static int searchedNodes = 0;

    private class Weights {
        public int PieceDifferential = 0;
        public int Mobility = 0;
        public int Corner = 0;
        public int Stability = 0;
    }

    private Weights weights = new Weights();

    public AI(AIDifficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Move findBestMove(Board board, boolean isBlackTurn) {
        int depth = getSearchDepth(board);
        List<Move> validMoves = board.getValidMoves(isBlackTurn);

        if (validMoves.isEmpty()) {
            return null; // No valid moves
        }
        Move bestMove = validMoves.get(0);
        if (isBlackTurn) {
            int bestScore = Integer.MIN_VALUE;
            for (Move move : validMoves) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.row, move.col, isBlackTurn);
                int score = minimax(newBoard, depth - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (Move move : validMoves) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.row, move.col, isBlackTurn);
                int score = minimax(newBoard, depth - 1, true, Integer.MIN_VALUE, Integer.MAX_VALUE);
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }
        return bestMove;
    }

    private int minimax(Board board, int depth, boolean isMaximizingPlayer, int alpha, int beta) {
        searchedNodes++;
        if (depth == 0 || board.isGameOver()) {
            return evaluateBoard(board.getState(), isMaximizingPlayer);
        }

        if (isMaximizingPlayer) {
            int bestValue = Integer.MIN_VALUE;
            for (Move move : board.getValidMoves(isMaximizingPlayer)) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.row, move.col, isMaximizingPlayer);
                int value = minimax(newBoard, depth - 1, false, alpha, beta);
                bestValue = Math.max(bestValue, value);
                alpha = Math.max(alpha, bestValue);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        } else {
            int bestValue = Integer.MAX_VALUE;
            for (Move move : board.getValidMoves(isMaximizingPlayer)) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.row, move.col, isMaximizingPlayer);
                int value = minimax(newBoard, depth - 1, true, alpha, beta);
                bestValue = Math.min(bestValue, value);
                beta = Math.min(beta, bestValue);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        }
    }

    private int evaluateBoard(int[][] boardState, boolean isBlackTurn) {
        int totalPieces = countTotalPieces(boardState, searchedNodes);
        boolean isLateGame = totalPieces > 56;
        weights.PieceDifferential = isLateGame ? 100 : 50;
        weights.Mobility = isLateGame ? 20 : 50;
        weights.Corner = isLateGame ? 200 : 100;
        weights.Stability = isLateGame ? 50 : 20;

        int evaluation = switch (difficulty) {
            case EASY -> evaluateBoardEasy(boardState, isBlackTurn);
            case MEDIUM -> evaluateBoardMedium(boardState, isBlackTurn);
            case HARD -> evaluateBoardHard(boardState, isBlackTurn);
            case EXPERT -> evaluateBoardExpert(boardState, isBlackTurn);
        };
        if (!isBlackTurn) {
            evaluation = -evaluation;
        }

        return evaluation;
    }

    private int evaluateBoardEasy(int[][] board, boolean isBlack) {
        return getPieceDifferentialScore(board);
    }

    private int evaluateBoardMedium(int[][] board, boolean isBlack) {
        return getPieceDifferentialScore(board) + getMobilityScore(board);
    }

    private int evaluateBoardHard(int[][] board, boolean isBlack) {
        return getPieceDifferentialScore(board) + getMobilityScore(board) + getCornerScore(board);
    }

    private int evaluateBoardExpert(int[][] board, boolean isBlack) {
        return getPieceDifferentialScore(board) + getMobilityScore(board) + getCornerScore(board) + getStabilityScore(board);
    }

    private int getPieceDifferentialScore(int[][] board) {
        int blackPieces = countPieces(board, 1);
        int whitePieces = countPieces(board, 2);
        return (blackPieces - whitePieces) * weights.PieceDifferential;
    }

    private int getMobilityScore(int[][] board) {
        int blackMoves = getValidMoves(board, true).size();
        int whiteMoves = getValidMoves(board, false).size();
        return (blackMoves - whiteMoves) * weights.Mobility;
    }

    private int getCornerScore(int[][] board) {
        int blackCorners = countCorners(board, 1);
        int whiteCorners = countCorners(board, 2);
        return (blackCorners - whiteCorners) * weights.Corner;
    }

    private int getStabilityScore(int[][] board) {
        int stabilityBlack = countStablePieces(board, 1);
        int stabilityWhite = countStablePieces(board, 2);
        return (stabilityBlack - stabilityWhite) * weights.Stability;
    }

    private int countTotalPieces(int[][] boardState, int color) {
        int count = 0;
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                if (boardState[i][j] != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countPieces(int[][] boardState, int color) {
        int count = 0;
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                if (boardState[i][j] == color) {
                    count++;
                }
            }
        }
        return count;
    }

    private List<Move> getValidMoves(int[][] boardState, boolean isBlackTurn) {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < Board.BOARD_SIZE; i++) {
            for (int j = 0; j < Board.BOARD_SIZE; j++) {
                if (Board.isValidMove(i, j, isBlackTurn, boardState)) {
                    moves.add(new Move(i, j));
                }
            }
        }
        return moves;
    }

    private int countCorners(int[][] boardState, int color) {
        int count = 0;
        if (boardState[0][0] == color)
            count++;
        if (boardState[0][Board.BOARD_SIZE - 1] == color)
            count++;
        if (boardState[Board.BOARD_SIZE - 1][0] == color)
            count++;
        if (boardState[Board.BOARD_SIZE - 1][Board.BOARD_SIZE - 1] == color)
            count++;
        return count;
    }

    private int countStablePieces(int[][] boardState, int color) {
        int stableCount = 0;
        int[][] corners = { { 0, 0 }, { 0, 7 }, { 7, 0 }, { 7, 7 } };
        int[][] directions = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { -1, 0 }, { 0, -1 }, { -1, -1 }, { 1, -1 }, { -1, 1 } };

        for (int[] corner : corners) {
            if (boardState[corner[0]][corner[1]] == color) {
                stableCount += 2;
                for (int[] dir : directions) {
                    int newRow = corner[0] + dir[0];
                    int newCol = corner[1] + dir[1];
                    if (newRow >= 0 && newRow < Board.BOARD_SIZE &&
                            newCol >= 0 && newCol < Board.BOARD_SIZE &&
                            boardState[newRow][newCol] == color) {
                        stableCount++;
                    }
                }
            }
        }

        return stableCount;
    }

    private int getSearchDepth(Board board) {
        int emptySpaces = board.countEmptySpaces();
        if (emptySpaces <= 10)
            return emptySpaces; // End game
        if (emptySpaces <= 16)
            return 8; // Late game
        if (emptySpaces <= 32)
            return 7; // Mid game
        return 6; // Early game
    }

    public static int getSearchNodes() {
        return searchedNodes;
    }
}