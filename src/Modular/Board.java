package Modular;

import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int BOARD_SIZE = 8;
    public static final int CELL_SIZE = 60;
    private int[][] state;

    public Board() {
        state = new int[BOARD_SIZE][BOARD_SIZE];
    }

    public Board(Board original) {
        this.state = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(original.state[i], 0, this.state[i], 0, BOARD_SIZE);
        }
    }

    public void initialize() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                state[i][j] = 0;
            }
        }
        int mid = BOARD_SIZE / 2;
        state[mid - 1][mid - 1] = 2; // White
        state[mid - 1][mid] = 1; // Black
        state[mid][mid - 1] = 1; // Black
        state[mid][mid] = 2; // White
    }

    public int[][] getState() {
        return state;
    }

    public static boolean isValidMove(int row, int col, boolean isBlackTurn, int[][] boardState) {
        if (boardState[row][col] != 0) {
            return false;
        }

        int player = isBlackTurn ? 1 : 2;
        int opponent = isBlackTurn ? 2 : 1;
        int[][] directions = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;

            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
                if (boardState[r][c] == opponent) {
                    foundOpponent = true;
                } else if (boardState[r][c] == player && foundOpponent) {
                    return true;
                } else {
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return false;
    }

    public void makeMove(int row, int col, boolean isBlackTurn) {
        if (!isValidMove(row, col, isBlackTurn, state)) {
            return;
        }

        int player = isBlackTurn ? 1 : 2;
        state[row][col] = player;
        flipPieces(row, col, player);
    }

    private void flipPieces(int row, int col, int player) {
        int opponent = (player == 1) ? 2 : 1;
        int[][] directions = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;
            List<Point> toFlip = new ArrayList<>();

            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
                if (state[r][c] == opponent) {
                    foundOpponent = true;
                    toFlip.add(new Point(r, c));
                } else if (state[r][c] == player && foundOpponent) {
                    for (Point p : toFlip) {
                        state[p.x][p.y] = player;
                    }
                    break;
                } else {
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
    }

    public List<Move> getValidMoves(boolean isBlackTurn) {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, isBlackTurn, state)) {
                    moves.add(new Move(i, j));
                }
            }
        }
        return moves;
    }

    public boolean isGameOver() {
        return !hasValidMoves(true) && !hasValidMoves(false);
    }

    public boolean hasValidMoves(boolean isBlackTurn) {
        return !getValidMoves(isBlackTurn).isEmpty();
    }

    public int countPieces(int color) {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (state[i][j] == color)
                    count++;
            }
        }
        return count;
    }

    public int countEmptySpaces() {
        return BOARD_SIZE * BOARD_SIZE - countPieces(1) - countPieces(2);
    }

    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}