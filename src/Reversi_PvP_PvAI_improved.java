import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
public class Reversi_PvP_PvAI_improved extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 60;
    private JButton[][] board;
    private int[][] gameState;
    private boolean isBlackTurn;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private boolean isAIMode = false;
    private boolean isAITurn = false;

    public Reversi_PvP_PvAI_improved() {
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

        // Count available moves and empty spaces
        int availableMoves = 0;
        int emptySpaces = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 0) emptySpaces++;
                if (isValidMove(i, j, false)) {
                    availableMoves++;
                }
            }
        }

        // Adjust depth based on game phase
        int depth;
        if (emptySpaces <= 8) depth = 6;      // End game
        else if (emptySpaces <= 16) depth = 5; // Late game
        else if (emptySpaces <= 32) depth = 4; // Mid game
        else depth = 3;                        // Early game

        // AI is always white (!isBlackTurn)
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, false)) {
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

        if (bestMove == null && hasValidMoves(false)) {
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (isValidMove(i, j, false)) {
                        return new Move(i, j);
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
        
        // Count pieces and empty cells
        int totalPieces = 0;
        int emptyCount = 0;
        int aiPieces = 0;
        int playerPieces = 0;
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 0) {
                    emptyCount++;
                } else {
                    totalPieces++;
                    if (board[i][j] == 2) aiPieces++;
                    else playerPieces++;
                }
            }
        }
        
        // End game strategy
        if (totalPieces >= 55) {
            return (aiPieces - playerPieces) * 1000;  // Pure piece difference
        }
        
        // Positional weights for different game phases
        int[][] earlyGameWeights = {
            {200, -100, 100, 50, 50, 100, -100, 200},  // Corners are extremely valuable
            {-100, -200, -50, -50, -50, -50, -200, -100},
            {100, -50, 1, 1, 1, 1, -50, 100},
            {50, -50, 1, 1, 1, 1, -50, 50},
            {50, -50, 1, 1, 1, 1, -50, 50},
            {100, -50, 1, 1, 1, 1, -50, 100},
            {-100, -200, -50, -50, -50, -50, -200, -100},
            {200, -100, 100, 50, 50, 100, -100, 200}
        };
        
        int[][] lateGameWeights = {
            {150, -50, 50, 30, 30, 50, -50, 150},
            {-50, -100, -20, -20, -20, -20, -100, -50},
            {50, -20, 1, 1, 1, 1, -20, 50},
            {30, -20, 1, 1, 1, 1, -20, 30},
            {30, -20, 1, 1, 1, 1, -20, 30},
            {50, -20, 1, 1, 1, 1, -20, 50},
            {-50, -100, -20, -20, -20, -20, -100, -50},
            {150, -50, 50, 30, 30, 50, -50, 150}
        };
        
        // Choose weights based on game phase
        int[][] weights = (totalPieces < 40) ? earlyGameWeights : lateGameWeights;
        
        // Calculate positional score
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 2) { // AI (White)
                    aiScore += weights[i][j];
                } else if (board[i][j] == 1) { // Player (Black)
                    playerScore += weights[i][j];
                }
            }
        }
        
        // Mobility (number of valid moves) evaluation
        int aiMobility = countValidMoves(board, false);  // White's moves
        int playerMobility = countValidMoves(board, true);  // Black's moves
        
        // Corner ownership
        int cornerScore = evaluateCorners(board);
        
        // Edge control
        int edgeScore = evaluateEdges(board);
        
        // Combine all factors with appropriate weights
        return (aiScore - playerScore) * 10 +            // Position weight
               (aiMobility - playerMobility) * 50 +      // Mobility weight
               cornerScore * 200 +                       // Corner weight
               edgeScore * 100;                         // Edge weight
    }

    private int countValidMoves(int[][] board, boolean isBlack) {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMoveOnBoard(board, i, j, isBlack)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int evaluateCorners(int[][] board) {
        int aiCorners = 0;
        int playerCorners = 0;
        int[][] corners = {{0,0}, {0,7}, {7,0}, {7,7}};
        
        for (int[] corner : corners) {
            if (board[corner[0]][corner[1]] == 2) aiCorners++;
            else if (board[corner[0]][corner[1]] == 1) playerCorners++;
        }
        
        return aiCorners - playerCorners;
    }

    private int evaluateEdges(int[][] board) {
        int aiEdges = 0;
        int playerEdges = 0;
        
        // Check top and bottom edges
        for (int j = 1; j < BOARD_SIZE-1; j++) {
            if (board[0][j] == 2) aiEdges++;
            else if (board[0][j] == 1) playerEdges++;
            if (board[BOARD_SIZE-1][j] == 2) aiEdges++;
            else if (board[BOARD_SIZE-1][j] == 1) playerEdges++;
        }
        
        // Check left and right edges
        for (int i = 1; i < BOARD_SIZE-1; i++) {
            if (board[i][0] == 2) aiEdges++;
            else if (board[i][0] == 1) playerEdges++;
            if (board[i][BOARD_SIZE-1] == 2) aiEdges++;
            else if (board[i][BOARD_SIZE-1] == 1) playerEdges++;
        }
        
        return aiEdges - playerEdges;
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
            new Reversi_PvP_PvAI_improved().setVisible(true);
        });
    }
}