package Modular;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AI {
    public AIDifficulty difficulty;
    private final GameEngine gameEngine;

    private final WeightType earlyGameWeights = new WeightType(50, 50, 100, 20);
    private final WeightType lateGameWeights = new WeightType(100, 20, 200, 150);
    private WeightType weights;

    public AI(AIDifficulty difficulty, GameEngine gameEngine) {
        this.difficulty = difficulty;
        this.gameEngine = gameEngine;
    }

    public Move findBestMove(Board board, boolean isBlackTurn) {
        int depth = getSearchDepth();
        List<Move> validMoves = board.getValidMoves(isBlackTurn);

        if (validMoves.isEmpty()) {
            return null; // No valid moves
        }

        List<Move> bestMoves = new ArrayList<>();
        int bestScore = isBlackTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : validMoves) {
            Board newBoard = new Board(board);
            newBoard.makeMove(move.row, move.col, isBlackTurn);
            int score = minimax(newBoard, depth - 1, !isBlackTurn, Integer.MIN_VALUE, Integer.MAX_VALUE);

            if (isBlackTurn) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMoves.clear();
                    bestMoves.add(move);
                } else if (score == bestScore) {
                    bestMoves.add(move);
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMoves.clear();
                    bestMoves.add(move);
                } else if (score == bestScore) {
                    bestMoves.add(move);
                }
            }
        }

        if (!bestMoves.isEmpty()) {
            // Randomly select one of the best moves
            Random random = new Random();
            return bestMoves.get(random.nextInt(bestMoves.size()));
        }

        return null;
    }

    private int minimax(Board board, int depth, boolean isMaximizingPlayer, int alpha, int beta) {
        gameEngine.searchedNodes++;
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
        int totalPieces = gameEngine.gameDepth;
        // int totalPieces = countTotalPieces(boardState, gameEngine.searchedNodes);
        boolean isLateGame = totalPieces > 56;
        weights = isLateGame ? lateGameWeights : earlyGameWeights;

        int evaluation = switch (difficulty) {
            case EASY -> evaluateBoardEasy(boardState);
            case MEDIUM -> evaluateBoardMedium(boardState);
            case HARD -> evaluateBoardHard(boardState);
            case EXPERT -> evaluateBoardExpert(boardState);
        };
        if (!isBlackTurn) {
            evaluation = -evaluation;
        }

        return evaluation;
    }

    private int evaluateBoardEasy(int[][] board) {
        return getPieceDifferentialScore(board);
    }

    private int evaluateBoardMedium(int[][] board ) {
        return getPieceDifferentialScore(board) + getMobilityScore(board);
    }

    private int evaluateBoardHard(int[][] board) {
        return getPieceDifferentialScore(board) + getMobilityScore(board) + getCornerScore(board);
    }

    private int evaluateBoardExpert(int[][] board ) {
        return getPieceDifferentialScore(board) + getMobilityScore(board) + getCornerScore(board)
                + getStabilityScore(board);
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

    private int countTotalPieces(int[][] boardState) {
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

    private int getSearchDepth() {
        int emptySpaces = Board.BOARD_SIZE * Board.BOARD_SIZE - gameEngine.gameDepth;
        if (emptySpaces <= 10)
            return emptySpaces; // End game
        if (emptySpaces <= 16)
            return 8; // Late game
        if (emptySpaces <= 32)
            return 7; // Mid game
        return 6; // Early game
    }
}