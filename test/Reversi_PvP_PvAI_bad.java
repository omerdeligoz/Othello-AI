import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
public class Reversi_PvP_PvAI_bad extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 60;
    private JButton[][] board;
    private int[][] gameState;
    private boolean isBlackTurn;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private boolean isAIMode = false;
    private boolean isAITurn = false;

    public Reversi_PvP_PvAI_bad() {
 // Ask for game mode
        int choice = JOptionPane.showOptionDialog(null,
            "Select Game Mode",
            "Reversi",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new String[]{"Player vs Player", "Player vs AI"},
            "Player vs Player");
        
        isAIMode = (choice == 1);
        
        setTitle("Reversi Game - " + (isAIMode ? "Player vs AI" : "Player vs Player"));
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
        // Prevent any moves during AI's turn
        if (isAIMode && !isBlackTurn) {
            return; // It's AI's turn (white's turn)
        }

        if (gameState[row][col] != 0 || !isValidMove(row, col, isBlackTurn)) {
            return;
        }

        // Place the piece and flip opponent's pieces
        int player = isBlackTurn ? 1 : 2;
        gameState[row][col] = player;
        flipPieces(row, col, player);

        // Update the game
        isBlackTurn = !isBlackTurn;
        updateBoard();

        // Check if next player can make a move
        if (!hasValidMoves(isBlackTurn)) {
            if (!hasValidMoves(!isBlackTurn)) {
                endGame();
            } else {
                isBlackTurn = !isBlackTurn;
                JOptionPane.showMessageDialog(this, "No valid moves available. Turn skipped.");
                updateBoard();
            }
        }

        updateStatus();

        // Handle AI move if it's AI's turn (white's turn)
        if (isAIMode && !isBlackTurn) {
            isAITurn = true;
            // Add a small delay before AI move
            Timer timer = new Timer(500, e -> {
                makeAIMove();
                ((Timer)e.getSource()).stop();
            });
            timer.start();
        }
    }


    private void makeAIMove() {
        if (!isAITurn) return;
        
        Move bestMove = findBestMove();
        if (bestMove != null) {
            // Make the AI move
            int player = 2; // AI is white
            gameState[bestMove.row][bestMove.col] = player;
            flipPieces(bestMove.row, bestMove.col, player);
            
            // Update game state
            isBlackTurn = true; // Switch back to player's turn
            updateBoard();
            
            // Check for valid moves
            if (!hasValidMoves(isBlackTurn)) {
                if (!hasValidMoves(!isBlackTurn)) {
                    endGame();
                } else {
                    isBlackTurn = !isBlackTurn;
                    JOptionPane.showMessageDialog(this, "No valid moves available. Turn skipped.");
                    updateBoard();
                }
            }
            
            updateStatus();
        }
        isAITurn = false;
    }

    private static class Move {
        int row, col;
        Move(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    private Move findBestMove() {
        int[][] boardCopy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(gameState[i], 0, boardCopy[i], 0, BOARD_SIZE);
        }

        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // First, count available moves
        int availableMoves = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, false)) { // Check for white's moves
                    availableMoves++;
                }
            }
        }

        // If near end game (few moves left), adjust evaluation depth
        int depth = availableMoves <= 7 ? 7 : 4;  // Increase depth for end game

        // AI is always white (!isBlackTurn)
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, false)) { // false because AI is white
                    // Try this move
                    int[][] newBoard = makeTemporaryMove(boardCopy, i, j, false);
                    int moveValue = minimax(newBoard, depth, false, alpha, beta);

                    if (moveValue > bestValue) {
                        bestValue = moveValue;
                        bestMove = new Move(i, j);
                    }
                    alpha = Math.max(alpha, bestValue);
                }
            }
        }

        // If no move found but there should be valid moves, try simple evaluation
        if (bestMove == null && hasValidMoves(false)) {
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (isValidMove(i, j, false)) {
                        return new Move(i, j); // Return first valid move
                    }
                }
            }
        }

        return bestMove;
    }

    private int minimax(int[][] board, int depth, boolean isMaximizing, int alpha, int beta) {
        if (depth == 0 || isGameOver(board)) {
            return evaluateBoard(board);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (isValidMoveOnBoard(board, i, j, !isBlackTurn)) {
                        int[][] newBoard = makeTemporaryMove(board, i, j, !isBlackTurn);
                        int eval = minimax(newBoard, depth - 1, false, alpha, beta);
                        maxEval = Math.max(maxEval, eval);
                        alpha = Math.max(alpha, eval);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (isValidMoveOnBoard(board, i, j, isBlackTurn)) {
                        int[][] newBoard = makeTemporaryMove(board, i, j, isBlackTurn);
                        int eval = minimax(newBoard, depth - 1, true, alpha, beta);
                        minEval = Math.min(minEval, eval);
                        beta = Math.min(beta, eval);
                        if (beta <= alpha) {
                            break;
                        }
                    }
                }
            }
            return minEval;
        }
    }

    
    private int[][] makeTemporaryMove(int[][] board, int row, int col, boolean isBlack) {
        int[][] newBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, newBoard[i], 0, BOARD_SIZE);
        }
        
        int player = isBlack ? 1 : 2;
        newBoard[row][col] = player;
        flipPiecesOnBoard(newBoard, row, col, player);
        return newBoard;
    }

    private void flipPiecesOnBoard(int[][] board, int row, int col, int player) {
        int opponent = (player == 1) ? 2 : 1;
        int[][] directions = {{-1,-1}, {-1,0}, {-1,1}, {0,-1}, {0,1}, {1,-1}, {1,0}, {1,1}};

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];
            boolean foundOpponent = false;
            java.util.List<Point> toFlip = new java.util.ArrayList<>();

            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE) {
                if (board[r][c] == opponent) {
                    foundOpponent = true;
                    toFlip.add(new Point(r, c));
                } else if (board[r][c] == player && foundOpponent) {
                    for (Point p : toFlip) {
                        board[p.x][p.y] = player;
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

    private boolean isValidMoveOnBoard(int[][] board, int row, int col, boolean isBlack) {
        if (board[row][col] != 0) {
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
                if (board[r][c] == opponent) {
                    foundOpponent = true;
                } else if (board[r][c] == player && foundOpponent) {
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

    private boolean isGameOver(int[][] board) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 0 && (isValidMoveOnBoard(board, i, j, true) || 
                    isValidMoveOnBoard(board, i, j, false))) {
                    return false;
                }
            }
        }
        return true;
    }

    private int evaluateBoard(int[][] board) {
        int aiScore = 0;
        int playerScore = 0;
        
        // Count pieces for end game evaluation
        int totalPieces = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != 0) totalPieces++;
            }
        }
        
        // If near end game, focus on piece count
        if (totalPieces >= 55) {  // Adjust this threshold as needed
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == 2) aiScore += 100;      // AI (White)
                    else if (board[i][j] == 1) playerScore += 100;  // Player (Black)
                }
            }
            return aiScore - playerScore;
        }
        
        // Regular positional evaluation for mid-game
        int[][] weights = {
            {100, -20, 10, 5, 5, 10, -20, 100},
            {-20, -50, -2, -2, -2, -2, -50, -20},
            {10, -2, 1, 1, 1, 1, -2, 10},
            {5, -2, 1, 1, 1, 1, -2, 5},
            {5, -2, 1, 1, 1, 1, -2, 5},
            {10, -2, 1, 1, 1, 1, -2, 10},
            {-20, -50, -2, -2, -2, -2, -50, -20},
            {100, -20, 10, 5, 5, 10, -20, 100}
        };
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 2) { // AI (White)
                    aiScore += weights[i][j];
                } else if (board[i][j] == 1) { // Player (Black)
                    playerScore += weights[i][j];
                }
            }
        }
        
        return aiScore - playerScore;
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
            new Reversi_PvP_PvAI_bad().setVisible(true);
        });
    }
}