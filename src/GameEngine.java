import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    private Board board; // The game board
    private final AI blackAI; // AI for the black player
    private final AI whiteAI; // AI for the white player
    private boolean isBlackTurn; // Indicates whose turn it is (true for black, false for white)
    private final Reversi ui; // Reference to the UI
    private final GameMode gameMode; // Current game mode (Human vs AI, AI vs AI, etc.)
    private int lastMoveRow = -1; // Row of the last move made
    private int lastMoveCol = -1; // Column of the last move made
    private long gameStartTime; // Time when the game started
    public int placedTiles = 4; // Number of tiles placed on the board (initialized to 4 for starting pieces)
    public int searchedNodes = 0; // Number of nodes searched by the AI (for performance tracking)
    private final List<MoveHistory> gameHistory = new ArrayList<>(); // History of moves for undo functionality

    // Constructor to initialize the game engine
    public GameEngine(Reversi ui, GameMode gameMode, AIDifficulty blackAIDifficulty, AIDifficulty whiteAIDifficulty) {
        this.ui = ui; // Set the UI reference
        this.gameMode = gameMode; // Set the game mode
        this.board = new Board(); // Initialize the board
        this.isBlackTurn = true; // Black starts first

        // Initialize AI players based on the game mode
        if (gameMode == GameMode.HumanvsAI || gameMode == GameMode.AIvsAI) {
            this.blackAI = (blackAIDifficulty != null) ? new AI(blackAIDifficulty, this) : null; // Initialize black AI
            this.whiteAI = (whiteAIDifficulty != null) ? new AI(whiteAIDifficulty, this) : null; // Initialize white AI
        } else {
            this.blackAI = null; // No AI for black in Human vs Human mode
            this.whiteAI = null; // No AI for white in Human vs Human mode
        }
    }

    // Start the game
    public void startGame() {
        gameStartTime = System.currentTimeMillis(); // Record the start time
        board.initialize(); // Initialize the board with starting pieces
        ui.updateBoard(board.getState(), isBlackTurn, -1, -1); // Update the UI with the initial board state
        ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore(), -1, -1); // Update the status message

        // If it's AI's turn, make the first move
        if (gameMode == GameMode.AIvsAI || (gameMode == GameMode.HumanvsAI && isBlackTurn && blackAI != null)) {
            makeAIMove(); // Trigger AI to make the first move
        }
    }

    // Make a move for the human player
    public void makePlayerMove(int row, int col) {
        if (Board.isValidMove(row, col, isBlackTurn, board.getState())) { // Check if the move is valid
            gameHistory.add(new MoveHistory(board, isBlackTurn, searchedNodes, placedTiles, new Move(lastMoveRow, lastMoveCol))); // Save the current state to history
            placedTiles++; // Increment the number of placed tiles
            board.makeMove(row, col, isBlackTurn); // Make the move on the board
            lastMoveRow = row; // Record the last move row
            lastMoveCol = col; // Record the last move column
            isBlackTurn = !isBlackTurn; // Switch turns
            ui.updateBoard(board.getState(), isBlackTurn, lastMoveRow, lastMoveCol); // Update the UI
            ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore(), lastMoveRow, lastMoveCol); // Update the status message

            // Check if the next player can make a move
            if (!board.hasValidMoves(isBlackTurn)) {
                if (!board.hasValidMoves(!isBlackTurn)) {
                    endGame(); // End the game if neither player can move
                } else {
                    isBlackTurn = !isBlackTurn; // Skip the turn if the next player has no valid moves
                    if (gameMode != GameMode.AIvsAI) { // Only show message in human games
                        ui.showErrorMessage("No valid moves available. Turn skipped.");
                    }
                    ui.updateBoard(board.getState(), isBlackTurn, lastMoveRow, lastMoveCol); // Update the UI
                }
            }

            ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore(), lastMoveRow, lastMoveCol); // Update the status message

            if (isAITurn()) { // If it's AI's turn, trigger the AI to make a move
                makeAIMove();
            }
        }
    }

    // Make a move for the AI player
    private void makeAIMove() {
        AI currentAI = isBlackTurn ? blackAI : whiteAI; // Determine which AI is making the move
        if (currentAI != null) {
            Move move = currentAI.findBestMove(board, isBlackTurn); // Find the best move using the AI
            Timer timer = new Timer(100, _ -> { // Add a 100 ms delay for AI move
                if (move != null) {
                    gameHistory.add(new MoveHistory(board, isBlackTurn, searchedNodes, placedTiles, move)); // Save the current state to history
                    placedTiles++; // Increment the number of placed tiles
                    board.makeMove(move.row, move.col, isBlackTurn); // Make the move on the board
                    lastMoveRow = move.row; // Record the last move row
                    lastMoveCol = move.col; // Record the last move column
                    isBlackTurn = !isBlackTurn; // Switch turns
                    ui.updateBoard(board.getState(), isBlackTurn, lastMoveRow, lastMoveCol); // Update the UI
                    ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore(), lastMoveRow, lastMoveCol); // Update the status message

                    // Check if the next player can make a move
                    if (!board.hasValidMoves(isBlackTurn)) {
                        if (!board.hasValidMoves(!isBlackTurn)) {
                            endGame(); // End the game if neither player can move
                        } else {
                            isBlackTurn = !isBlackTurn; // Skip the turn if the next player has no valid moves
                            if (gameMode != GameMode.AIvsAI) { // Only show message in human games
                                ui.showErrorMessage("No valid moves available. Turn skipped.");
                            }
                            ui.updateBoard(board.getState(), isBlackTurn, lastMoveRow, lastMoveCol); // Update the UI
                        }
                    }

                    ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore(), lastMoveRow, lastMoveCol); // Update the status message
                    if (isAITurn()) { // If it's AI's turn again, trigger the next move
                        makeAIMove();
                    }
                }
            });
            timer.setRepeats(false); // Ensure the timer only runs once
            timer.start(); // Start the timer
        }
    }

    // Check if it's the AI's turn
    private boolean isAITurn() {
        return (gameMode == GameMode.AIvsAI) || // Both players are AI
                (gameMode == GameMode.HumanvsAI // One player is AI
                        && ((isBlackTurn && blackAI != null) || (!isBlackTurn && whiteAI != null)));
    }

    // Undo the last move
    public void undoMove() {
        if (!gameHistory.isEmpty()) {
            MoveHistory lastState = gameHistory.removeLast(); // Get the last state from history
            board = new Board(lastState.boardState); // Restore the board state
            isBlackTurn = lastState.isBlackTurn; // Restore the turn
            searchedNodes = lastState.searchedNodes; // Restore the number of searched nodes
            placedTiles = lastState.gameDepth; // Restore the number of placed tiles
            lastMoveRow = lastState.lastMove.row; // Restore the last move row
            lastMoveCol = lastState.lastMove.col; // Restore the last move column
            ui.updateBoard(board.getState(), isBlackTurn, -1, -1); // Update the UI
            ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore(), lastMoveRow, lastMoveCol); // Update the status message
        }
    }

    // Get the current score as a formatted string
    private String getScore() {
        int blackCount = board.countPieces(1); // Count black pieces
        int whiteCount = board.countPieces(2); // Count white pieces
        String blackPlayer = getPlayerType(blackAI); // Get the type of black player (AI or Human)
        String whitePlayer = getPlayerType(whiteAI); // Get the type of white player (AI or Human)

        return String.format("Black (%s): %d  White (%s): %d", blackPlayer, blackCount, whitePlayer, whiteCount);
    }

    // End the game and determine the winner
    private void endGame() {
        int blackCount = board.countPieces(1); // Count black pieces
        int whiteCount = board.countPieces(2); // Count white pieces
        String winner = blackCount > whiteCount ? "Black" : whiteCount > blackCount ? "White" : "Tie"; // Determine the winner
        String player1 = getPlayerType(blackAI); // Get the type of black player
        String player2 = getPlayerType(whiteAI); // Get the type of white player
        GameLogger.logGameResult(gameMode.name(), player1, player2, gameStartTime, searchedNodes, blackCount,
                whiteCount, winner); // Log the game result
        ui.showEndGameDialog(winner, blackCount, whiteCount); // Show the end game dialog
    }

    // Get the type of player (AI or Human) based on the game mode
    private String getPlayerType(AI ai) {
        if (gameMode == GameMode.AIvsAI) {
            return "AI (" + (ai == blackAI ? blackAI.difficulty : whiteAI.difficulty) + ")"; // Both players are AI
        } else if (gameMode == GameMode.HumanvsAI && ai != null && ai == whiteAI) {
            return "AI (" + whiteAI.difficulty + ")"; // White is AI
        } else if (gameMode == GameMode.HumanvsAI && ai != null && ai == blackAI) {
            return "AI (" + blackAI.difficulty + ")"; // Black is AI
        } else {
            return "Human"; // Human player
        }
    }
}