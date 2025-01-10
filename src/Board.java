import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int BOARD_SIZE = 8; // Size of the board (8x8)
    public static final int CELL_SIZE = 60; // Size of each cell in the board (for UI purposes)
    private final int[][] state; // 2D array representing the board state

    // Constructor to initialize an empty board
    public Board() {
        state = new int[BOARD_SIZE][BOARD_SIZE];
    }

    // Copy constructor to create a deep copy of the board
    public Board(Board original) {
        this.state = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(original.state[i], 0, this.state[i], 0, BOARD_SIZE);
        }
    }

    // Initialize the board with the starting configuration
    public void initialize() {
        // Clear the board
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                state[i][j] = 0; // 0 represents an empty cell
            }
        }

        // Set up the initial pieces in the center
        int mid = BOARD_SIZE / 2;
        state[mid - 1][mid - 1] = 2; // White piece
        state[mid - 1][mid] = 1; // Black piece
        state[mid][mid - 1] = 1; // Black piece
        state[mid][mid] = 2; // White piece
    }

    // Get the current state of the board
    public int[][] getState() {
        return state;
    }

    // Check if a move is valid for a given player
    public static boolean isValidMove(int row, int col, boolean isBlackTurn, int[][] boardState) {
        if (boardState[row][col] != 0) {
            return false; // Cell is not empty, move is invalid
        }

        int player = isBlackTurn ? 1 : 2; // Player's piece (1 for black, 2 for white)
        int opponent = isBlackTurn ? 2 : 1; // Opponent's piece
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}}; // All 8 possible directions

        // Check each direction for a valid move
        for (int[] direction : directions) {
            int newRow = row + direction[0];
            int newCol = col + direction[1];
            boolean foundOpponent = false;

            // Traverse in the current direction
            while (newRow >= 0 && newRow < BOARD_SIZE && newCol >= 0 && newCol < BOARD_SIZE) {
                if (boardState[newRow][newCol] == opponent) {
                    foundOpponent = true; // Found an opponent's piece
                } else if (boardState[newRow][newCol] == player && foundOpponent) {
                    return true; // Valid move: a line of opponent pieces is flanked by the player's piece
                } else {
                    break; // Invalid move: no flanking or empty cell
                }
                newRow += direction[0];
                newCol += direction[1];
            }
        }
        return false; // No valid move found in any direction
    }

    // Make a move on the board for a given player
    public void makeMove(int row, int col, boolean isBlackTurn) {
        if (!isValidMove(row, col, isBlackTurn, state)) {
            return; // Move is invalid, do nothing
        }

        int player = isBlackTurn ? 1 : 2; // Player's piece
        state[row][col] = player; // Place the player's piece
        flipPieces(row, col, player); // Flip the opponent's pieces
    }

    // Flip the opponent's pieces after a valid move
    private void flipPieces(int row, int col, int player) {
        int opponent = (player == 1) ? 2 : 1; // Opponent's piece
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}}; // All 8 possible directions

        // Check each direction for pieces to flip
        for (int[] direction : directions) {
            int newRow = row + direction[0];
            int newCol = col + direction[1];
            boolean foundOpponent = false;
            List<Point> toFlip = new ArrayList<>(); // List of pieces to flip

            // Traverse in the current direction
            while (newRow >= 0 && newRow < BOARD_SIZE && newCol >= 0 && newCol < BOARD_SIZE) {
                if (state[newRow][newCol] == opponent) {
                    foundOpponent = true; // Found an opponent's piece
                    toFlip.add(new Point(newRow, newCol)); // Add to the list of pieces to flip
                } else if (state[newRow][newCol] == player && foundOpponent) {
                    // Flip all opponent pieces in this direction
                    for (Point p : toFlip) {
                        state[p.x][p.y] = player;
                    }
                    break;
                } else {
                    break; // No more pieces to flip in this direction
                }
                newRow += direction[0];
                newCol += direction[1];
            }
        }
    }

    // Get all valid moves for a given player
    public List<Move> getValidMoves(boolean isBlackTurn) {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, isBlackTurn, state)) {
                    moves.add(new Move(i, j)); // Add valid move to the list
                }
            }
        }
        return moves;
    }

    // Check if the game is over (no valid moves for either player)
    public boolean isGameOver() {
        return !hasValidMoves(true) && !hasValidMoves(false);
    }

    // Check if a given player has any valid moves
    public boolean hasValidMoves(boolean isBlackTurn) {
        return !getValidMoves(isBlackTurn).isEmpty();
    }

    // Count the number of pieces of a specific color on the board
    public int countPieces(int color) {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (state[i][j] == color)
                    count++; // Increment count for each matching piece
            }
        }
        return count;
    }

    // Count the number of empty spaces on the board
    public int countEmptySpaces() {
        return BOARD_SIZE * BOARD_SIZE - countPieces(1) - countPieces(2);
    }

    // Inner class to represent a point (row, col) on the board
    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}