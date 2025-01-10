import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AI {
    public AIDifficulty difficulty; // Difficulty level of the AI
    private final GameEngine gameEngine; // Reference to the game engine

    private final WeightType weights = new WeightType(1, 2, 1, 1); // Weights for different evaluation factors

    // Constructor to initialize AI with difficulty and game engine
    public AI(AIDifficulty difficulty, GameEngine gameEngine) {
        this.difficulty = difficulty;
        this.gameEngine = gameEngine;
    }

    // Method to find the best move for the current board state
    public Move findBestMove(Board board, boolean isBlackTurn) {
        int depth = getSearchDepth(); // Determine the search depth based on the game phase
        List<Move> validMoves = board.getValidMoves(isBlackTurn); // Get all valid moves for the current player

        if (validMoves.isEmpty()) {
            return null; // No valid moves available
        }

        List<Move> bestMoves = new ArrayList<>(); // List to store the best moves
        int bestScore = isBlackTurn ? Integer.MIN_VALUE : Integer.MAX_VALUE; // Initialize best score based on player

        // Evaluate each valid move
        for (Move move : validMoves) {
            Board newBoard = new Board(board); // Create a new board state
            newBoard.makeMove(move.row, move.col, isBlackTurn); // Make the move on the new board
            int score = minimax(newBoard, depth - 1, !isBlackTurn, Integer.MIN_VALUE, Integer.MAX_VALUE); // Get the score using minimax

            // Update the best move based on the score
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

        // Randomly select one of the best moves if there are multiple
        if (!bestMoves.isEmpty()) {
            Random random = new Random();
            return bestMoves.get(random.nextInt(bestMoves.size()));
        }

        return null;
    }

    // Minimax algorithm with alpha-beta pruning to evaluate moves
    private int minimax(Board board, int depth, boolean isMaximizingPlayer, int alpha, int beta) {
        gameEngine.searchedNodes++; // Increment the number of nodes searched
        if (depth == 0 || board.isGameOver()) {
            return evaluateBoard(board.getState(), isMaximizingPlayer); // Evaluate the board if depth is 0 or game is over
        }

        if (isMaximizingPlayer) {
            int bestValue = Integer.MIN_VALUE;
            for (Move move : board.getValidMoves(isMaximizingPlayer)) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.row, move.col, isMaximizingPlayer);
                int value = minimax(newBoard, depth - 1, false, alpha, beta); // Recursively call minimax for minimizing player
                bestValue = Math.max(bestValue, value);
                alpha = Math.max(alpha, bestValue);
                if (beta <= alpha) {
                    break; // Alpha-beta pruning
                }
            }
            return bestValue;
        } else {
            int bestValue = Integer.MAX_VALUE;
            for (Move move : board.getValidMoves(isMaximizingPlayer)) {
                Board newBoard = new Board(board);
                newBoard.makeMove(move.row, move.col, isMaximizingPlayer);
                int value = minimax(newBoard, depth - 1, true, alpha, beta); // Recursively call minimax for maximizing player
                bestValue = Math.min(bestValue, value);
                beta = Math.min(beta, bestValue);
                if (beta <= alpha) {
                    break; // Alpha-beta pruning
                }
            }
            return bestValue;
        }
    }

    // Evaluate the board based on the difficulty level
    private int evaluateBoard(int[][] boardState, boolean isBlackTurn) {
        int evaluation = switch (difficulty) {
            case EASY -> evaluateBoardEasy(boardState);
            case MEDIUM -> evaluateBoardMedium(boardState);
            case HARD -> evaluateBoardHard(boardState);
            case EXPERT -> evaluateBoardExpert(boardState);
        };
        if (!isBlackTurn) {
            evaluation = -evaluation; // Invert evaluation for white player
        }

        return evaluation;
    }

    // Evaluate the board for easy difficulty (only piece differential)
    private int evaluateBoardEasy(int[][] board) {
        return getPieceDifferentialScore(board);
    }

    // Evaluate the board for medium difficulty (piece differential and mobility)
    private int evaluateBoardMedium(int[][] board) {
        return getPieceDifferentialScore(board) + getMobilityScore(board);
    }

    // Evaluate the board for hard difficulty (piece differential, mobility, and corners)
    private int evaluateBoardHard(int[][] board) {
        return getPieceDifferentialScore(board) + getMobilityScore(board) + getCornerScore(board);
    }

    // Evaluate the board for expert difficulty (piece differential, mobility, corners, and stability)
    private int evaluateBoardExpert(int[][] board) {
        return getPieceDifferentialScore(board) + getMobilityScore(board) + getCornerScore(board) + getStabilityScore(board);
    }

    // Calculate the piece differential score (difference in number of pieces)
    private int getPieceDifferentialScore(int[][] board) {
        int blackPieces = countPieces(board, 1);
        int whitePieces = countPieces(board, 2);
        return (blackPieces - whitePieces) * weights.PieceDifferential;
    }

    // Calculate the mobility score (difference in number of valid moves)
    private int getMobilityScore(int[][] board) {
        int blackMoves = getValidMoves(board, true).size();
        int whiteMoves = getValidMoves(board, false).size();
        return (blackMoves - whiteMoves) * weights.Mobility;
    }

    // Calculate the corner score (difference in number of corner pieces)
    private int getCornerScore(int[][] board) {
        int blackCorners = countCorners(board, 1);
        int whiteCorners = countCorners(board, 2);
        return (blackCorners - whiteCorners) * weights.Corner;
    }

    // Calculate the stability score (difference in number of stable pieces)
    private int getStabilityScore(int[][] board) {
        int stabilityBlack = countStablePieces(board, 1);
        int stabilityWhite = countStablePieces(board, 2);
        return (stabilityBlack - stabilityWhite) * weights.Stability;
    }

    // Count the total number of pieces on the board
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

    // Count the number of pieces of a specific color
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

    // Get all valid moves for a player
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

    // Count the number of corner pieces for a specific color
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

    // Count the number of stable pieces for a specific color
    private int countStablePieces(int[][] boardState, int color) {
        int stableCount = 0;
        int[][] corners = {{0, 0}, {0, 7}, {7, 0}, {7, 7}};
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};

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

    // Determine the search depth based on the number of empty spaces
    private int getSearchDepth() {
        int emptySpaces = Board.BOARD_SIZE * Board.BOARD_SIZE - gameEngine.placedTiles;
        if (emptySpaces <= 13)
            return emptySpaces; // End game: search all remaining moves
        if (emptySpaces <= 16)
            return 10; // Late game: search depth 10
        if (emptySpaces <= 32)
            return 8; // Mid-game: search depth 8
        return 7; // Early game: search depth 7
    }
}