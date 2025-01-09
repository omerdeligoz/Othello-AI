import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;

public class Reversi extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int CELL_SIZE = 60;
    private static final int TIME_DELAY = 100;
    private JButton[][] board;
    private int[][] gameState;
    private boolean isBlackTurn;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private JLabel statisticsLabel;
    private boolean isAITurn = false;
    private AIDifficulty blackAIDifficulty;
    private AIDifficulty whiteAIDifficulty;
    private boolean isAIBlack = false;
    private long gameStartTime;
    private static final String LOG_DIRECTORY = "game_logs";
    private static final String LOG_FILE = "game_history.txt";
    private GameMode gameMode;
    private int searchedNodes = 0;

    private static class Move {
        int row, col;

        Move(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    public enum AIDifficulty {
        EASY, MEDIUM, HARD, EXPERT
    }

    private enum WeightType {
        Mobility, Corner, PieceDifferential
    }

    public enum GameMode {
        PvsP, PvsAI, AIvsAI
    }

    private Map<WeightType, Integer> weightMap = Map.of(
            WeightType.Mobility, 10,
            WeightType.PieceDifferential, 5,
            WeightType.Corner, 25);

    // Weight matrices for position scoring
    private static final int[][] earlyGameWeights = {
            { 200, -20, 20, 5, 5, 20, -20, 200 },
            { -20, -40, -5, -5, -5, -5, -40, -20 },
            { 20, -5, 15, 3, 3, 15, -5, 20 },
            { 5, -5, 3, 3, 3, 3, -5, 5 },
            { 5, -5, 3, 3, 3, 3, -5, 5 },
            { 20, -5, 15, 3, 3, 15, -5, 20 },
            { -20, -40, -5, -5, -5, -5, -40, -20 },
            { 200, -20, 20, 5, 5, 20, -20, 200 }
    };

    private static final int[][] lateGameWeights = {
            { 100, 50, 50, 50, 50, 50, 50, 100 },
            { 50, 30, 20, 20, 20, 20, 30, 50 },
            { 50, 20, 10, 10, 10, 10, 20, 50 },
            { 50, 20, 10, 5, 5, 10, 20, 50 },
            { 50, 20, 10, 5, 5, 10, 20, 50 },
            { 50, 20, 10, 10, 10, 10, 20, 50 },
            { 50, 30, 20, 20, 20, 20, 30, 50 },
            { 100, 50, 50, 50, 50, 50, 50, 100 }
    };

    public Reversi() {
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
        String[] options = { "Player vs Player", "Player vs AI", "AI vs AI" };
        int choice = JOptionPane.showOptionDialog(null,
                "Select Game Mode",
                "Reversi",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        gameMode = GameMode.values()[choice];

        if (gameMode == GameMode.PvsAI || gameMode == GameMode.AIvsAI) {
            String[] difficulties = { "Easy", "Medium", "Hard", "Expert" };

            if (gameMode == GameMode.PvsAI) {
                // Let player choose their color
                int colorChoice = JOptionPane.showOptionDialog(null,
                        "Choose your color",
                        "Color Selection",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[] { "Black", "White" },
                        "Black");

                isAIBlack = (colorChoice == 1); // AI is black if player chose white

                int aiDifficulty = JOptionPane.showOptionDialog(null,
                        "Select AI Difficulty",
                        "AI Difficulty",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        difficulties,
                        difficulties[1]);
                if (isAIBlack) {
                    blackAIDifficulty = AIDifficulty.values()[aiDifficulty];
                } else {
                    whiteAIDifficulty = AIDifficulty.values()[aiDifficulty];
                }
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
        if (gameMode == GameMode.AIvsAI) {
            setTitle("Reversi - AI(" + blackAIDifficulty + ") vs AI(" + whiteAIDifficulty + ")");
        } else if (gameMode == GameMode.PvsAI) {
            setTitle("Reversi - Player vs AI(" + (isAIBlack ? blackAIDifficulty : whiteAIDifficulty) + ")");
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
        scoreLabel = new JLabel("Black: 2  White: 2  Empty: 60");
        statisticsLabel = new JLabel("Searched Nodes: 0");
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.add(statusLabel);
        infoPanel.add(scoreLabel);
        infoPanel.add(statisticsLabel);

        add(boardPanel, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.SOUTH);

        // Set up initial game state
        initializeGame();

        pack();
        setLocationRelativeTo(null);
    }

    public Reversi(GameMode gameMode, AIDifficulty blackAIDifficulty, AIDifficulty whiteAIDifficulty) {
        this();
        this.gameMode = gameMode;
        this.blackAIDifficulty = blackAIDifficulty;
        this.whiteAIDifficulty = whiteAIDifficulty;
        setTitle("Reversi - AI(" + blackAIDifficulty + ") vs AI(" + whiteAIDifficulty + ")");
    }

    public void startGame() {
        initializeGame();
        while (!isGameOver(gameState)) {
            makeAIMove();
        }
    }

    public String getWinner() {
        int blackCount = 0;
        int whiteCount = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 1)
                    blackCount++;
                else if (gameState[i][j] == 2)
                    whiteCount++;
            }
        }
        if (blackCount > whiteCount) {
            return "Black";
        } else if (whiteCount > blackCount) {
            return "White";
        } else {
            return "Tie";
        }
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
                writer.println("Searched Nodes: " + searchedNodes);
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
        if (gameMode == GameMode.AIvsAI) {
            return "AI vs AI";
        } else if (gameMode == GameMode.PvsAI) {
            return "Player vs AI";
        } else {
            return "Player vs Player";
        }
    }

    private String getPlayerType(boolean isBlack) {
        if (gameMode == GameMode.AIvsAI) {
            return "AI (" + (isBlack ? blackAIDifficulty : whiteAIDifficulty) + ")";
        } else if (gameMode == GameMode.PvsAI && !isBlack) {
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
        gameState[mid - 1][mid] = 1; // Black
        gameState[mid][mid - 1] = 1; // Black
        gameState[mid][mid] = 2; // White

        updateBoard();
        updateStatus();

        // Start AI vs AI mode if selected
        if (gameMode == GameMode.AIvsAI || (gameMode == GameMode.PvsAI &&
                ((isAIBlack && isBlackTurn) || (!isAIBlack && !isBlackTurn)))) {
            isAITurn = true;
            makeDelayedAIMove();
        }
    }

    private void makeMove(int row, int col) {
        // In AI vs AI mode, ignore player clicks
        if (gameMode == GameMode.AIvsAI) {
            return;
        }

        // Prevent moves during AI's turn
        if ((gameMode == GameMode.PvsAI &&
                ((isAIBlack && isBlackTurn) || (!isAIBlack && !isBlackTurn))) ||
                gameMode == GameMode.AIvsAI) {
            return;
        }

        // If the move is not valid, ignore it
        if (gameState[row][col] != 0 || !isValidMove(gameState, row, col, isBlackTurn)) {
            return;
        }

        makeActualMove(row, col);
    }

    private void makeActualMove(int row, int col) {
        // Place the piece and flip opponent's pieces
        int player = isBlackTurn ? 1 : 2;
        gameState[row][col] = player;
        flipPieces(gameState, row, col, player);

        // Update the game
        isBlackTurn = !isBlackTurn;
        updateBoard();

        // Check if next player can make a move
        if (!hasValidMoves(isBlackTurn)) {
            if (!hasValidMoves(!isBlackTurn)) {
                endGame();
            } else {
                isBlackTurn = !isBlackTurn;
                if (gameMode != GameMode.AIvsAI) { // Only show message in human games
                    JOptionPane.showMessageDialog(this, "No valid moves available. Turn skipped.");
                }
                updateBoard();
            }
        }

        updateStatus();

    }

    private void makeAIMove() {
        if (!isAITurn)
            return;

        Move bestMove = null;
        if (gameMode == GameMode.AIvsAI || gameMode == GameMode.PvsAI) {
            // In AI vs AI mode, each AI uses its own strategy
            bestMove = findBestMove(isBlackTurn);
        }

        if (bestMove != null) {
            makeActualMove(bestMove.row, bestMove.col);
        }
        isAITurn = false;

        // Trigger the next AI move if in AI vs AI mode
        if ((gameMode == GameMode.PvsAI &&
                ((isAIBlack && isBlackTurn) || (!isAIBlack && !isBlackTurn))) ||
                gameMode == GameMode.AIvsAI) {
            isAITurn = true;
            makeDelayedAIMove();
        }
    }

    private void makeDelayedAIMove() {
        Timer timer = new Timer(TIME_DELAY, e -> {
            makeAIMove();
            ((Timer) e.getSource()).stop();
        });
        timer.start();
    }

    private Move findBestMove(boolean isBlack) {
        int[][] boardCopy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(gameState[i], 0, boardCopy[i], 0, BOARD_SIZE);
        }

        List<Move> bestMoves = new ArrayList<>();
        int bestValue = isBlack ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // Count empty spaces
        int emptySpaces = countEmptySpaces(gameState);

        // Get appropriate depth based on game phase
        int depth = getInitialDepth(emptySpaces);

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(gameState, i, j, isBlack)) {
                    searchedNodes++;
                    int[][] newBoard = makeTemporaryMove(boardCopy, i, j, isBlack);
                    int moveValue = minimax(newBoard, depth, !isBlack, alpha, beta);

                    if ((isBlack && moveValue < bestValue) || (!isBlack && moveValue > bestValue)) {
                        bestValue = moveValue;
                        bestMoves.clear();
                        bestMoves.add(new Move(i, j));
                    } else if (moveValue == bestValue) {
                        bestMoves.add(new Move(i, j));
                    }

                    if (isBlack) {
                        beta = Math.min(beta, bestValue);
                    } else {
                        alpha = Math.max(alpha, bestValue);
                    }
                }
            }
        }

        if (!bestMoves.isEmpty()) {
            // Randomly select from the best moves
            return bestMoves.get(new Random().nextInt(bestMoves.size()));
        }
        return findFallbackMove(isBlack);
    }

    private Move findFallbackMove(boolean isBlack) {
        // Simple fallback to find any valid move if we run out of time
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(gameState, i, j, isBlack)) {
                    return new Move(i, j);
                }
            }
        }
        return null;
    }

    private int getInitialDepth(int emptySpaces) {
        if (emptySpaces <= 10)
            return emptySpaces; // End game
        if (emptySpaces <= 16)
            return 8; // Late game
        if (emptySpaces <= 32)
            return 7; // Mid game
        return 6; // Early game
    }

    private int minimax(int[][] board, int depth, boolean isMaximizing, int alpha, int beta) {
        if (depth == 0 || isGameOver(board)) {
            return evaluateBoard(board);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (isValidMove(board, i, j, !isBlackTurn)) {
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
                    if (isValidMove(board, i, j, isBlackTurn)) {
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
        int[][] tempBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, tempBoard[i], 0, BOARD_SIZE);
        }

        int player = isBlack ? 1 : 2;
        tempBoard[row][col] = player;
        flipPieces(tempBoard, row, col, player);
        return tempBoard;
    }

    private void flipPieces(int[][] targetBoard, int row, int col, int player) {
        int opponent = (player == 1) ? 2 : 1;
        int[][] directions = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };

        for (int[] direction : directions) {
            int newRow = row + direction[0];
            int newCol = col + direction[1];
            boolean foundOpponent = false;
            List<Point> toFlip = new ArrayList<>();

            while (newRow >= 0 && newRow < BOARD_SIZE && newCol >= 0 && newCol < BOARD_SIZE) {
                if (targetBoard[newRow][newCol] == opponent) {
                    foundOpponent = true;
                    toFlip.add(new Point(newRow, newCol));
                } else if (targetBoard[newRow][newCol] == player && foundOpponent) {
                    for (Point p : toFlip) {
                        targetBoard[p.x][p.y] = player;
                    }
                    break;
                } else {
                    break;
                }
                newRow += direction[0];
                newCol += direction[1];
            }
        }
    }

    private boolean isValidMove(int[][] board, int row, int col, boolean isBlack) {
        if (board[row][col] != 0) {
            return false;
        }

        int player = isBlack ? 1 : 2;
        int opponent = isBlack ? 2 : 1;
        int[][] directions = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };

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
                if (board[i][j] == 0 && (isValidMove(board, i, j, true) ||
                        isValidMove(board, i, j, false))) {
                    return false;
                }
            }
        }
        return true;
    }

    private int evaluateBoard(int[][] board) {
        if (gameMode == GameMode.AIvsAI) {
            if (isBlackTurn) {
                int score = evaluateBoardForDifficulty(board, true, blackAIDifficulty);
                return -score; // Negate for black's perspective
            } else {
                return evaluateBoardForDifficulty(board, false, whiteAIDifficulty);
            }
        } else if (gameMode == GameMode.PvsAI) {
            // In Player vs AI mode, evaluate based on AI's color
            return evaluateBoardForDifficulty(board, isAIBlack, isAIBlack ? blackAIDifficulty : whiteAIDifficulty);
        }
        return 0; // For PvsP mode
    }

    private int evaluateBoardForDifficulty(int[][] board, boolean isBlack, AIDifficulty difficulty) {
        return switch (difficulty) {
            case EASY -> evaluateBoardEasy(board, isBlack);
            case MEDIUM -> evaluateBoardMedium(board, isBlack);
            case HARD -> evaluateBoardHard(board, isBlack);
            case EXPERT -> evaluateBoardExpert(board, isBlack);
        };
    }

    private int evaluateBoardEasy(int[][] board, boolean isBlack) {
        int player = isBlack ? 1 : 2;
        int opponent = isBlack ? 2 : 1;
        return getPieceDifferentialScore(board, player, opponent);
    }

    private int evaluateBoardMedium(int[][] board, boolean isBlack) {
        int player = isBlack ? 1 : 2;
        int opponent = isBlack ? 2 : 1;

        // Positional scoring
        int positionalScore = getPositionalScore(board, player, opponent);

        // Basic mobility
        int mobilityScore = getMobilityScore(board, isBlack);

        return positionalScore + mobilityScore;
    }

    private int evaluateBoardHard(int[][] board, boolean isBlack) {
        int player = isBlack ? 1 : 2;
        int opponent = isBlack ? 2 : 1;

        int positionalScore = getPositionalScore(board, player, opponent);

        // Improved mobility scoring
        int mobilityScore = getMobilityScore(board, isBlack);

        // Corner control
        int cornerScore = getCornerScore(board, player, opponent);

        // Edge control
        int edgeScore = getEdgeScore(board, player, opponent);

        // Late game piece advantage
        int totalPieces = countTotalPieces(board);
        boolean isLateGame = totalPieces > 50;
        int pieceDifferencialScore = 0;
        if (isLateGame) {
            pieceDifferencialScore = getPieceDifferentialScore(board, player, opponent);
        }

        return positionalScore + mobilityScore + cornerScore + edgeScore + pieceDifferencialScore;
    }

    private int evaluateBoardExpert(int[][] board, boolean isBlack) {
        int player = isBlack ? 1 : 2;
        int opponent = isBlack ? 2 : 1;
        int emptySpaces = countEmptySpaces(board);

        // Use exact calculation for endgame
        if (emptySpaces <= 10) {
            return calculateExactScore(board, isBlack);
        }
        int totalPieces = countTotalPieces(board);
        boolean isLateGame = totalPieces >= 50;
        int positionalScore = getPositionalScore(board, player, opponent);
        int mobilityScore = getMobilityScore(board, isBlack);
        int cornerScore = getCornerScore(board, player, opponent);
        int edgeScore = getEdgeScore(board, player, opponent);
        int stabilityScore = getStabilityScore(board, player);

        // Strategic late game
        int pieaceDifferencialScore = 0;
        if (isLateGame) {
            pieaceDifferencialScore = getPieceDifferentialScore(board, player, opponent);

            // Frontier discs penalty
            pieaceDifferencialScore -= countFrontierDiscs(board, player);
        }

        return positionalScore + mobilityScore + cornerScore + edgeScore + stabilityScore + pieaceDifferencialScore;
    }

    private int getPieceDifferentialScore(int[][] board, int player, int opponent) {
        int playerPieces = 0;
        int opponentPieces = 0;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == player)
                    playerPieces++;
                else if (board[i][j] == opponent)
                    opponentPieces++;
            }
        }
        return playerPieces - opponentPieces;
    }

    private int getMobilityScore(int[][] board, boolean isBlack) {
        return countValidMoves(board, isBlack) - countValidMoves(board, !isBlack);
    }

    private int getPositionalScore(int[][] board, int player, int opponent) {
        int totalPieces = countTotalPieces(board);
        boolean isEarlyGame = totalPieces < 40;
        int positionalScore = 0;
        // Position scoring with basic weights
        int[][] weights = isEarlyGame ? earlyGameWeights : lateGameWeights;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == player) {
                    positionalScore += weights[i][j];
                } else if (board[i][j] == opponent) {
                    positionalScore -= weights[i][j];
                }
            }
        }
        return positionalScore;
    }

    List<Move> getAllValidMoves(int[][] board, boolean isBlack) {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (isValidMove(board, i, j, isBlack)) {
                    moves.add(new Move(i, j));
                }
            }
        }
        return moves;
    }

    private int calculateExactScore(int[][] board, boolean isBlack) {
        List<Move> moves = getAllValidMoves(board, isBlack);

        if (moves.isEmpty()) {
            // If game is over, return actual piece difference * large number
            int player = isBlack ? 1 : 2;
            int opponent = isBlack ? 2 : 1;
            int playerPieces = countPieces(board, player);
            int opponentPieces = countPieces(board, opponent);
            return (playerPieces - opponentPieces) * 10000;
        }

        int bestScore = Integer.MIN_VALUE;
        for (Move move : moves) {
            int[][] newBoard = makeMove(board, move, isBlack);
            int score = -calculateExactScore(newBoard, !isBlack);
            bestScore = Math.max(bestScore, score);
        }

        return bestScore;
    }

    private int[][] makeMove(int[][] board, Move move, boolean isBlack) {
        int[][] newBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            newBoard[i] = board[i].clone();
        }
        // Apply move and flip pieces
        applyMove(newBoard, move, isBlack);
        return newBoard;
    }

    private void applyMove(int[][] board, Move move, boolean isBlack) {
        int player = isBlack ? 1 : 2;
        board[move.row][move.col] = player;

        int[] dx = { -1, -1, -1, 0, 0, 1, 1, 1 };
        int[] dy = { -1, 0, 1, -1, 1, -1, 0, 1 };

        for (int dir = 0; dir < 8; dir++) {
            int x = move.row + dx[dir];
            int y = move.col + dy[dir];
            List<Point> toFlip = new ArrayList<>();

            while (isValidPosition(x, y) && board[x][y] != 0 && board[x][y] != player) {
                toFlip.add(new Point(x, y));
                x += dx[dir];
                y += dy[dir];
            }

            if (isValidPosition(x, y) && board[x][y] == player) {
                for (Point p : toFlip) {
                    board[p.x][p.y] = player;
                }
            }
        }
    }

    private int countFrontierDiscs(int[][] board, int player) {
        int frontierCount = 0;
        int[] dx = { -1, -1, -1, 0, 0, 1, 1, 1 };
        int[] dy = { -1, 0, 1, -1, 1, -1, 0, 1 };

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == player) {
                    // Check if disc is adjacent to empty space
                    for (int dir = 0; dir < 8; dir++) {
                        int newI = i + dx[dir];
                        int newJ = j + dy[dir];
                        if (isValidPosition(newI, newJ) && board[newI][newJ] == 0) {
                            frontierCount++;
                            break;
                        }
                    }
                }
            }
        }
        return frontierCount;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    // Helper method to check if a position is a corner
    private boolean isCorner(int row, int col) {
        return (row == 0 || row == 7) && (col == 0 || col == 7);
    }

    private int countTotalPieces(int[][] board) {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] != 0)
                    count++;
            }
        }
        return count;
    }

    private int countEmptySpaces(int[][] board) {
        return BOARD_SIZE * BOARD_SIZE - countTotalPieces(board);
    }

    private int countPieces(int[][] board, int color) {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == color)
                    count++;
            }
        }
        return count;
    }

    private int evaluateCornerVulnerabilities(int[][] board, int myColor, int opponentColor) {
        int[][] nearCorners = {
                { 0, 1 }, { 1, 0 }, { 1, 1 }, { 0, 6 }, { 1, 6 }, { 1, 7 },
                { 6, 0 }, { 6, 1 }, { 7, 1 }, { 6, 6 }, { 6, 7 }, { 7, 6 }
        };

        int vulnerabilityScore = 0;
        for (int[] pos : nearCorners) {
            if (board[pos[0]][pos[1]] == myColor)
                vulnerabilityScore++;
            else if (board[pos[0]][pos[1]] == opponentColor)
                vulnerabilityScore--;
        }

        return vulnerabilityScore;
    }

    private int getCornerScore(int[][] board, int playerColor, int opponentColor) {
        int playerCorners = 0;
        int opponentCorners = 0;
        int[][] corners = { { 0, 0 }, { 0, 7 }, { 7, 0 }, { 7, 7 } };

        for (int[] corner : corners) {
            if (board[corner[0]][corner[1]] == playerColor)
                playerCorners++;
            else if (board[corner[0]][corner[1]] == opponentColor)
                opponentCorners++;
        }

        return playerCorners - opponentCorners;
    }

    private int getEdgeScore(int[][] board, int myColor, int opponentColor) {
        int myEdges = 0;
        int opponentEdges = 0;

        // Check edges excluding corners
        for (int i = 1; i < BOARD_SIZE - 1; i++) {
            if (board[0][i] == myColor)
                myEdges++;
            else if (board[0][i] == opponentColor)
                opponentEdges++;

            if (board[7][i] == myColor)
                myEdges++;
            else if (board[7][i] == opponentColor)
                opponentEdges++;

            if (board[i][0] == myColor)
                myEdges++;
            else if (board[i][0] == opponentColor)
                opponentEdges++;

            if (board[i][7] == myColor)
                myEdges++;
            else if (board[i][7] == opponentColor)
                opponentEdges++;
        }

        return myEdges - opponentEdges;
    }

    private int getStabilityScore(int[][] board, int color) {
        int stableCount = 0;

        // Check corners and their adjacent stable pieces
        int[][] corners = { { 0, 0 }, { 0, 7 }, { 7, 0 }, { 7, 7 } };
        int[][] directions = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { -1, 0 }, { 0, -1 }, { -1, -1 }, { 1, -1 }, { -1, 1 } };

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
                if (isValidMove(board, i, j, isBlack)) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean hasValidMoves(boolean isBlack) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 0 && isValidMove(gameState, i, j, isBlack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j].setBackground(new Color(0, 120, 0)); // Reset background
                if (gameState[i][j] == 1) {
                    board[i][j].setIcon(createDiskIcon(Color.BLACK));
                } else if (gameState[i][j] == 2) {
                    board[i][j].setIcon(createDiskIcon(Color.WHITE));
                } else {
                    board[i][j].setIcon(null);
                    // Highlight valid moves with a lighter green color
                    if (isValidMove(gameState, i, j, isBlackTurn)) {
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
                if (gameState[i][j] == 1)
                    blackCount++;
                else if (gameState[i][j] == 2)
                    whiteCount++;
            }
        }
        statusLabel.setText(isBlackTurn ? "Black's turn" : "White's turn");
        scoreLabel.setText(String.format("Black: %d  White: %d  Remaining: %d", blackCount, whiteCount,
                BOARD_SIZE * BOARD_SIZE - (blackCount + whiteCount)));
        statisticsLabel.setText(String.format("Searched Nodes: %d", searchedNodes));
    }

    private void endGame() {
        int blackCount = 0;
        int whiteCount = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (gameState[i][j] == 1)
                    blackCount++;
                else if (gameState[i][j] == 2)
                    whiteCount++;
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
                new String[] { "Return to Mode Selection" },
                "Return to Mode Selection");

        if (result == 0) {
            showModeChoosingScreen();
        }
    }

    private void showModeChoosingScreen() {
        dispose(); // Close current window
        SwingUtilities.invokeLater(() -> {
            new Reversi().setVisible(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Reversi().setVisible(true);
        });
    }
}