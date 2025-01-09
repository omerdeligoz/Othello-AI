import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class Reversi_complete extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 60;
    private JButton[][] board;
    private int[][] gameState;
    private boolean isBlackTurn;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private boolean isAIMode = false;
    private boolean isAIvsAIMode = false;
    private boolean isAITurn = false;
    private AIDifficulty blackAIDifficulty;
    private AIDifficulty whiteAIDifficulty;
    private long gameStartTime;
    private static final String LOG_DIRECTORY = "game_logs";
    private static final String LOG_FILE = "game_history.txt";
    
    private enum AIDifficulty {
        EASY, MEDIUM, HARD, EXPERT
    }
    
    private static final int[][] earlyGameWeights = {
        {200, -100, 100, 50, 50, 100, -100, 200},  // Corners are extremely valuable
        {-100, -200, -50, -50, -50, -50, -200, -100},
        {100, -50, 1, 1, 1, 1, -50, 100},
        {50, -50, 1, 1, 1, 1, -50, 50},
        {50, -50, 1, 1, 1, 1, -50, 50},
        {100, -50, 1, 1, 1, 1, -50, 100},
        {-100, -200, -50, -50, -50, -50, -200, -100},
        {200, -100, 100, 50, 50, 100, -100, 200}
    };
    
    private static final int[][] lateGameWeights = {
        {150, -50, 50, 30, 30, 50, -50, 150},
        {-50, -100, -20, -20, -20, -20, -100, -50},
        {50, -20, 1, 1, 1, 1, -20, 50},
        {30, -20, 1, 1, 1, 1, -20, 30},
        {30, -20, 1, 1, 1, 1, -20, 30},
        {50, -20, 1, 1, 1, 1, -20, 50},
        {-50, -100, -20, -20, -20, -20, -100, -50},
        {150, -50, 50, 30, 30, 50, -50, 150}
    };
    
    public Reversi_complete() {
        super("Reversi");
        gameStartTime = System.currentTimeMillis();
        
        // Create logs directory if it doesn't exist
        new File(LOG_DIRECTORY).mkdirs();
        // Create log file if it doesn't exist
        File logFile = new File(LOG_DIRECTORY, LOG_FILE);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Ask for game mode
        String[] options = {"Player vs Player", "Player vs AI", "AI vs AI"};
        int choice = JOptionPane.showOptionDialog(null,
        "Select Game Mode",
        "Reversi",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        options,
        options[0]);
        
        isAIMode = (choice == 1);
        isAIvsAIMode = (choice == 2);
        
        if (isAIMode || isAIvsAIMode) {
            String[] difficulties = {"Easy", "Medium", "Hard", "Expert"};
            
            if (isAIMode) {
                int aiDifficulty = JOptionPane.showOptionDialog(null,
                "Select AI Difficulty",
                "AI Difficulty",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                difficulties,
                difficulties[1]);
                whiteAIDifficulty = AIDifficulty.values()[aiDifficulty];
            } else {
                // AI vs AI mode
                int blackDifficulty = JOptionPane.showOptionDialog(null,
                "Select Black AI Difficulty",
                "Black AI Difficulty",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                difficulties,
                difficulties[1]);
                blackAIDifficulty = AIDifficulty.values()[blackDifficulty];
                
                int whiteDifficulty = JOptionPane.showOptionDialog(null,
                "Select White AI Difficulty",
                "White AI Difficulty",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                difficulties,
                difficulties[1]);
                whiteAIDifficulty = AIDifficulty.values()[whiteDifficulty];
            }
        }
        if (isAIvsAIMode) {
            setTitle("Reversi - AI(" + blackAIDifficulty + ") vs AI(" + whiteAIDifficulty + ")");
        } else if (isAIMode) {
            setTitle("Reversi - Player vs AI(" + whiteAIDifficulty + ")");
        } else {
            setTitle("Reversi - Player vs Player");
        }
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
    
    private void logGameResult(int blackCount, int whiteCount, String winner) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            String logFileName = LOG_DIRECTORY + "/" + LOG_FILE;
            
            // Append to the log file
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(logFileName, true))) {
                writer.println("\nGame Log - " + timestamp);
                writer.println("----------------------------------------");
                writer.println("Game Mode: " + getGameMode());
                writer.println("Game Duration: " + formatDuration(System.currentTimeMillis() - gameStartTime));
                writer.println("Black Player: " + getPlayerType(true));
                writer.println("White Player: " + getPlayerType(false));
                writer.println("Final Score:");
                writer.println("  Black: " + blackCount);
                writer.println("  White: " + whiteCount);
                writer.println("Winner: " + winner);
                writer.println("----------------------------------------");
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getGameMode() {
        if (isAIvsAIMode) {
            return "AI vs AI";
        } else if (isAIMode) {
            return "Player vs AI";
        } else {
            return "Player vs Player";
        }
    }
    
    private String getPlayerType(boolean isBlack) {
        if (isAIvsAIMode) {
            return "AI (" + (isBlack ? blackAIDifficulty : whiteAIDifficulty) + ")";
        } else if (isAIMode && !isBlack) {
            return "AI (" + whiteAIDifficulty + ")";
        } else {
            return "Human Player";
        }
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d minutes, %d seconds", minutes, seconds);
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
        gameState[mid - 1][mid - 1] = 2; // White
        gameState[mid - 1][mid] = 1;   // Black
        gameState[mid][mid - 1] = 1;   // Black
        gameState[mid][mid] = 2;     // White
        
        updateBoard();
        updateStatus();
        
        // Start AI vs AI mode if selected
        if (isAIvsAIMode) {
            isAITurn = true;
            SwingUtilities.invokeLater(() -> {
                makeAIMove();
            });
        }
    }
    
    private void makeMove(int row, int col) {
        // In AI vs AI mode, ignore player clicks
        if (isAIvsAIMode) {
            return;
        }
        
        // Prevent moves during AI's turn in PvAI mode
        if (isAIMode && !isBlackTurn) {
            return;
        }
        
        if (gameState[row][col] != 0 || !isValidMove(row, col, isBlackTurn)) {
            return;
        }
        
        makeActualMove(row, col);
    }
    
    private void makeActualMove(int row, int col) {
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
                if (!isAIvsAIMode) { // Only show message in human games
                    JOptionPane.showMessageDialog(this, "No valid moves available. Turn skipped.");
                }
                updateBoard();
            }
        }
        
        updateStatus();
        
        // Handle AI moves
        if ((isAIMode && !isBlackTurn) || isAIvsAIMode) {
            isAITurn = true;
            SwingUtilities.invokeLater(() -> {
                makeAIMove();
            });
        }
    }
    
    
    private void makeAIMove() {
        if (!isAITurn) return;
        
        Move bestMove = null;
        if (isAIvsAIMode) {
            // In AI vs AI mode, each AI uses its own strategy
            bestMove = isBlackTurn ? findBestMoveForBlack() : findBestMove();
        } else {
            // In Player vs AI mode, AI is always white
            bestMove = findBestMove();
        }
        
        if (bestMove != null) {
            makeActualMove(bestMove.row, bestMove.col);
        }
        isAITurn = false;
        
        // Trigger the next AI move if in AI vs AI mode
        if (isAIvsAIMode) {
            isAITurn = true;
            SwingUtilities.invokeLater(() -> {
                makeAIMove();
            });
        }
    }
    
    private Move findBestMoveForBlack() {
        int[][] boardCopy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(gameState[i], 0, boardCopy[i], 0, BOARD_SIZE);
        }
        
        List<Move> bestMoves = new ArrayList<>();
        int bestValue = Integer.MAX_VALUE;  // Black AI minimizes
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        
        // Count empty spaces
        int emptySpaces = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 0) emptySpaces++;
            }
        }
        
        // Get appropriate depth based on game phase
        int depth = getInitialDepth(emptySpaces);
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, true)) {
                    int[][] newBoard = makeTemporaryMove(boardCopy, i, j, true);
                    int moveValue = minimax(newBoard, depth, true, alpha, beta);
                    
                    if (moveValue < bestValue) {  // Found better move
                        bestValue = moveValue;
                        bestMoves.clear();
                        bestMoves.add(new Move(i, j));
                    } else if (moveValue == bestValue) {  // Found equally good move
                        bestMoves.add(new Move(i, j));
                    }
                    beta = Math.min(beta, bestValue);
                }
            }
        }
        
        if (!bestMoves.isEmpty()) {
            // Randomly select from the best moves
            return bestMoves.get(new Random().nextInt(bestMoves.size()));
        }
        return findFallbackMove(true);
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
        
        List<Move> bestMoves = new ArrayList<>();
        int bestValue = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        
        // Count empty spaces
        int emptySpaces = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 0) emptySpaces++;
            }
        }
        
        // Get appropriate depth based on game phase
        int depth = getInitialDepth(emptySpaces);
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, false)) {
                    int[][] newBoard = makeTemporaryMove(boardCopy, i, j, false);
                    int moveValue = minimax(newBoard, depth, false, alpha, beta);
                    
                    if (moveValue > bestValue) {  // Found better move
                        bestValue = moveValue;
                        bestMoves.clear();
                        bestMoves.add(new Move(i, j));
                    } else if (moveValue == bestValue) {  // Found equally good move
                        bestMoves.add(new Move(i, j));
                    }
                    alpha = Math.max(alpha, bestValue);
                }
            }
        }
        
        if (!bestMoves.isEmpty()) {
            // Randomly select from the best moves
            return bestMoves.get(new Random().nextInt(bestMoves.size()));
        }
        return findFallbackMove(false);
    }
    
    private Move findFallbackMove(boolean isBlack) {
        // Simple fallback to find any valid move if we run out of time
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(i, j, isBlack)) {
                    return new Move(i, j);
                }
            }
        }
        return null;
    }
    
    private int getInitialDepth(int emptySpaces) {
        if (emptySpaces <= 8) return 8;      // End game
        if (emptySpaces <= 16) return 7;     // Late game
        if (emptySpaces <= 32) return 6;     // Mid game
        return 5;                            // Early game
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
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        
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
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        
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
        if (isAIvsAIMode) {
            if (isBlackTurn) {
                int score = evaluateBoardForDifficulty(board, true, blackAIDifficulty);
                return -score; // Negate for black's perspective
            } else {
                return evaluateBoardForDifficulty(board, false, whiteAIDifficulty);
            }
        }
        return evaluateBoardForDifficulty(board, false, whiteAIDifficulty);
    }
    
    
    private int evaluateBoardForDifficulty(int[][] board, boolean isBlack, AIDifficulty difficulty) {
        switch (difficulty) {
            case EASY:
            return evaluateBoardEasy(board, isBlack);
            case MEDIUM:
            return evaluateBoardMedium(board, isBlack);
            case HARD:
            return evaluateBoardHard(board, isBlack);
            case EXPERT:
            return evaluateBoardExpert(board, isBlack);
            default:
            return evaluateBoardMedium(board, isBlack);
        }
    }
    
    private int evaluateBoardEasy(int[][] board, boolean isBlack) {
        int myColor = isBlack ? 1 : 2;
        int opponentColor = isBlack ? 2 : 1;
        
        // Simple piece counting with slight randomization
        int myPieces = 0;
        int opponentPieces = 0;
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == myColor) myPieces++;
                else if (board[i][j] == opponentColor) opponentPieces++;
            }
        }
        
        // Add small random factor to make it less predictable and weaker
        int randomFactor = new Random().nextInt(5) - 2; // Random value between -2 and 2
        return (myPieces - opponentPieces) * 5 + randomFactor;
    }
    
    private int evaluateBoardMedium(int[][] board, boolean isBlack) {
        int myColor = isBlack ? 1 : 2;
        int opponentColor = isBlack ? 2 : 1;
        
        int score = 0;
        
        // Basic piece counting
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == myColor) {
                    score += 10;
                    // Only consider corners
                    if ((i == 0 || i == 7) && (j == 0 || j == 7)) {
                        score += 50;
                    }
                } else if (board[i][j] == opponentColor) {
                    score -= 10;
                    if ((i == 0 || i == 7) && (j == 0 || j == 7)) {
                        score -= 50;
                    }
                }
            }
        }
        
        return score;
    }
    
    private int evaluateBoardHard(int[][] board, boolean isBlack) {
        int myColor = isBlack ? 1 : 2;
        int opponentColor = isBlack ? 2 : 1;
        
        int totalPieces = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != 0) totalPieces++;
            }
        }
        
        boolean isEarlyGame = totalPieces < 40;
        
        // Position score using weights
        int score = 0;
        int[][] weights = isEarlyGame ? earlyGameWeights : lateGameWeights;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == myColor) {
                    score += weights[i][j];
                } else if (board[i][j] == opponentColor) {
                    score -= weights[i][j];
                }
            }
        }
        
        // Mobility
        int myMoves = countValidMoves(board, isBlack);
        int opponentMoves = countValidMoves(board, !isBlack);
        score += (myMoves - opponentMoves) * 15;
        
        // Corner control
        score += evaluateCornersForColor(board, myColor, opponentColor) * 100;
        
        return score;
    }
    
    private int evaluateBoardExpert(int[][] board, boolean isBlack) {
        int myColor = isBlack ? 1 : 2;
        int opponentColor = isBlack ? 2 : 1;
        
        int totalPieces = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != 0) totalPieces++;
            }
        }
        
        boolean isEarlyGame = totalPieces < 40;
        boolean isLateGame = totalPieces >= 50;
        
        // Base position score
        int score = 0;
        int[][] weights = isEarlyGame ? earlyGameWeights : lateGameWeights;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == myColor) {
                    score += weights[i][j] * 2;  // Double the weight importance
                } else if (board[i][j] == opponentColor) {
                    score -= weights[i][j] * 2;
                }
            }
        }
        
        // Enhanced mobility scoring
        int myMoves = countValidMoves(board, isBlack);
        int opponentMoves = countValidMoves(board, !isBlack);
        score += (myMoves - opponentMoves) * (isEarlyGame ? 30 : 15);
        
        // Stronger corner control
        score += evaluateCornersForColor(board, myColor, opponentColor) * 200;
        
        // Edge control
        score += evaluateEdgesForColor(board, myColor, opponentColor) * 50;
        
        // Stability analysis
        score += evaluateStablePieces(board, myColor) * (isEarlyGame ? 75 : 150);
        
        // Corner adjacency penalty in early game
        if (isEarlyGame) {
            score += evaluateCornerVulnerabilities(board, myColor, opponentColor) * -75;
        }
        
        // Piece advantage only matters in late game
        if (isLateGame) {
            int myPieces = 0, opponentPieces = 0;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == myColor) myPieces++;
                    else if (board[i][j] == opponentColor) opponentPieces++;
                }
            }
            score += (myPieces - opponentPieces) * 100;
        }
        
        return score;
    }
    
    private int evaluateCornerVulnerabilities(int[][] board, int myColor, int opponentColor) {
        int[][] nearCorners = {
            {0, 1}, {1, 0}, {1, 1}, {0, 6}, {1, 6}, {1, 7},
            {6, 0}, {6, 1}, {7, 1}, {6, 6}, {6, 7}, {7, 6}
        };
        
        int vulnerabilityScore = 0;
        for (int[] pos : nearCorners) {
            if (board[pos[0]][pos[1]] == myColor) vulnerabilityScore++;
            else if (board[pos[0]][pos[1]] == opponentColor) vulnerabilityScore--;
        }
        
        return vulnerabilityScore;
    }
    
    private int evaluateCornersForColor(int[][] board, int myColor, int opponentColor) {
        int myCorners = 0;
        int opponentCorners = 0;
        int[][] corners = {{0, 0}, {0, 7}, {7, 0}, {7, 7}};
        
        for (int[] corner : corners) {
            if (board[corner[0]][corner[1]] == myColor) myCorners++;
            else if (board[corner[0]][corner[1]] == opponentColor) opponentCorners++;
        }
        
        return myCorners - opponentCorners;
    }
    
    private int evaluateEdgesForColor(int[][] board, int myColor, int opponentColor) {
        int myEdges = 0;
        int opponentEdges = 0;
        
        // Check edges excluding corners
        for (int i = 1; i < BOARD_SIZE - 1; i++) {
            if (board[0][i] == myColor) myEdges++;
            else if (board[0][i] == opponentColor) opponentEdges++;
            
            if (board[7][i] == myColor) myEdges++;
            else if (board[7][i] == opponentColor) opponentEdges++;
            
            if (board[i][0] == myColor) myEdges++;
            else if (board[i][0] == opponentColor) opponentEdges++;
            
            if (board[i][7] == myColor) myEdges++;
            else if (board[i][7] == opponentColor) opponentEdges++;
        }
        
        return myEdges - opponentEdges;
    }
    
    
    private int evaluateStablePieces(int[][] board, int color) {
        int stableCount = 0;
        
        // Check corners and their adjacent stable pieces
        int[][] corners = {{0, 0}, {0, 7}, {7, 0}, {7, 7}};
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}};
        
        for (int[] corner : corners) {
            if (board[corner[0]][corner[1]] == color) {
                stableCount += 2;
                // Check adjacent pieces
                for (int[] dir : directions) {
                    int newRow = corner[0] + dir[0];
                    int newCol = corner[1] + dir[1];
                    if (newRow >= 0 && newRow < BOARD_SIZE &&
                    newCol >= 0 && newCol < BOARD_SIZE &&
                    board[newRow][newCol] == color) {
                        stableCount++;
                    }
                }
            }
        }
        
        return stableCount;
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

    
    private boolean isValidMove(int row, int col, boolean isBlack) {
        if (gameState[row][col] != 0) {
            return false;
        }
        
        int player = isBlack ? 1 : 2;
        int opponent = isBlack ? 2 : 1;
        
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        
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
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
        
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
            winner = "Black";
        } else if (whiteCount > blackCount) {
            winner = "White";
        } else {
            winner = "Tie";
        }
        
        // Log the game result
        logGameResult(blackCount, whiteCount, winner);
        
        int result = JOptionPane.showOptionDialog(this,
        String.format("Game Over!\nBlack: %d\nWhite: %d\n%s wins!", blackCount, whiteCount, winner),
        "Game Over",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.INFORMATION_MESSAGE,
        null,
        new String[]{"Return to Mode Selection"},
        "Return to Mode Selection");
        
        if (result == 0) {
            showModeChoosingScreen();
        }
    }
    
    private void showModeChoosingScreen() {
        dispose(); // Close current window
        SwingUtilities.invokeLater(() -> {
            new Reversi_complete().setVisible(true);
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Reversi_complete().setVisible(true);
        });
    }
}