import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
public class Reversi_PvsP extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 60;
    private JButton[][] board;
    private int[][] gameState;
    private boolean isBlackTurn;
    private JLabel statusLabel;
    private JLabel scoreLabel;

    public Reversi_PvsP() {
        setTitle("Reversi Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize the game board
        JPanel boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        board = new JButton[BOARD_SIZE][BOARD_SIZE];
        gameState = new int[BOARD_SIZE][BOARD_SIZE];
        isBlackTurn = true;

        // Create the board buttons
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = new JButton();
                board[i][j].setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
                board[i][j].setBackground(new Color(0, 120, 0));
                final int row = i;
                final int col = j;
                board[i][j].addActionListener(e -> makeMove(row, col));
                boardPanel.add(board[i][j]);
            }
        }

        // Initialize status and score labels
        statusLabel = new JLabel("Black's turn");
        scoreLabel = new JLabel("Black: 2  White: 2");
        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.add(statusLabel);
        infoPanel.add(scoreLabel);

        add(boardPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);

        // Set up initial game state
        initializeGame();

        pack();
        setLocationRelativeTo(null);
    }

    private void initializeGame() {
        // Clear the board
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                gameState[i][j] = 0;
                board[i][j].setIcon(null);
            }
        }

        // Set up initial pieces
        int mid = BOARD_SIZE / 2;
        gameState[mid-1][mid-1] = 2; // White
        gameState[mid-1][mid] = 1;   // Black
        gameState[mid][mid-1] = 1;   // Black
        gameState[mid][mid] = 2;     // White

        updateBoard();
        updateStatus();
    }

    private void makeMove(int row, int col) {
        if (gameState[row][col] != 0 || !isValidMove(row, col, isBlackTurn)) {
            return;
        }

        // Place the piece and flip opponent's pieces
        int player = isBlackTurn ? 1 : 2;
        gameState[row][col] = player;
        flipPieces(row, col, player);

        // Update the game
        isBlackTurn = !isBlackTurn;
        updateBoard();  // Move this after changing turns
        
        // Check if next player can make a move
        if (!hasValidMoves(isBlackTurn)) {
            if (!hasValidMoves(!isBlackTurn)) {
                // Game is over
                endGame();
            } else {
                isBlackTurn = !isBlackTurn;
                JOptionPane.showMessageDialog(this, "No valid moves available. Turn skipped.");
                updateBoard();  // Add this to refresh after skipping turn
            }
        }

        updateStatus();
    }

    private boolean isValidMove(int row, int col, boolean isBlack) {
        if (gameState[row][col] != 0) {
            return false;
        }

        int player = isBlack ? 1 : 2;
        int opponent = isBlack ? 2 : 1;

        int[][] directions = {{-1,-1}, {-1,0}, {-1,1}, {0,-1}, {0,1}, {1,-1}, {1,0}, {1,1}};

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;

            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
                if (gameState[r][c] == opponent) {
                    foundOpponent = true;
                } else if (gameState[r][c] == player && foundOpponent) {
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

    private void flipPieces(int row, int col, int player) {
        int opponent = (player == 1) ? 2 : 1;
        int[][] directions = {{-1,-1}, {-1,0}, {-1,1}, {0,-1}, {0,1}, {1,-1}, {1,0}, {1,1}};

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;
            java.util.List<Point> toFlip = new java.util.ArrayList<>();

            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
                if (gameState[r][c] == opponent) {
                    foundOpponent = true;
                    toFlip.add(new Point(r, c));
                } else if (gameState[r][c] == player && foundOpponent) {
                    for (Point p : toFlip) {
                        gameState[p.x][p.y] = player;
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

    private boolean hasValidMoves(boolean isBlack) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 0 && isValidMove(i, j, isBlack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j].setBackground(new Color(0, 120, 0));  // Reset background
                if (gameState[i][j] == 1) {
                    board[i][j].setIcon(createDiskIcon(Color.BLACK));
                } else if (gameState[i][j] == 2) {
                    board[i][j].setIcon(createDiskIcon(Color.WHITE));
                } else {
                    board[i][j].setIcon(null);
                    // Highlight valid moves with a lighter green color
                    if (isValidMove(i, j, isBlackTurn)) {
                        board[i][j].setBackground(new Color(0, 180, 0));
                    }
                }
            }
        }
    }

    private ImageIcon createDiskIcon(Color color) {
        int size = CELL_SIZE - 10;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);
        g2d.dispose();
        return new ImageIcon(image);
    }

    private void updateStatus() {
        int blackCount = 0;
        int whiteCount = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 1) blackCount++;
                else if (gameState[i][j] == 2) whiteCount++;
            }
        }
        statusLabel.setText(isBlackTurn ? "Black's turn" : "White's turn");
        scoreLabel.setText(String.format("Black: %d  White: %d", blackCount, whiteCount));
    }

    private void endGame() {
        int blackCount = 0;
        int whiteCount = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 1) blackCount++;
                else if (gameState[i][j] == 2) whiteCount++;
            }
        }
        
        String winner;
        if (blackCount > whiteCount) {
            winner = "Black wins!";
        } else if (whiteCount > blackCount) {
            winner = "White wins!";
        } else {
            winner = "It's a tie!";
        }
        
        JOptionPane.showMessageDialog(this, 
            String.format("Game Over!\nBlack: %d\nWhite: %d\n%s", 
            blackCount, whiteCount, winner));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Reversi_PvsP().setVisible(true);
        });
    }
}