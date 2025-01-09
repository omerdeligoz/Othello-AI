import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    private Board board;
    private final AI blackAI;
    private final AI whiteAI;
    private boolean isBlackTurn;
    private final Reversi ui;
    private final GameMode gameMode;
    private int lastMoveRow = -1;
    private int lastMoveCol = -1;
    private long gameStartTime;
    public int placedTiles = 4;
    public int searchedNodes = 0;
    private final List<MoveHistory> gameHistory = new ArrayList<>();

    public GameEngine(Reversi ui, GameMode gameMode, AIDifficulty blackAIDifficulty, AIDifficulty whiteAIDifficulty) {
        this.ui = ui;
        this.gameMode = gameMode;
        this.board = new Board();
        this.isBlackTurn = true;

        // Initialize AI players based on game mode
        if (gameMode == GameMode.HumanvsAI || gameMode == GameMode.AIvsAI) {
            this.blackAI = (blackAIDifficulty != null) ? new AI(blackAIDifficulty, this) : null;
            this.whiteAI = (whiteAIDifficulty != null) ? new AI(whiteAIDifficulty, this) : null;
        } else {
            this.blackAI = null;
            this.whiteAI = null;
        }
    }

    public void startGame() {
        gameStartTime = System.currentTimeMillis();
        board.initialize();
        ui.updateBoard(board.getState(), isBlackTurn, -1, -1);
        ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore());

        // If it's AI's turn, make the first move
        if (gameMode == GameMode.AIvsAI || (gameMode == GameMode.HumanvsAI && isBlackTurn && blackAI != null)) {
            makeAIMove();
        }
    }

    public void makePlayerMove(int row, int col) {
        if (Board.isValidMove(row, col, isBlackTurn, board.getState())) {
            gameHistory.add(new MoveHistory(board, isBlackTurn, searchedNodes, placedTiles));
            placedTiles++;
            board.makeMove(row, col, isBlackTurn);
            lastMoveRow = row;
            lastMoveCol = col;
            isBlackTurn = !isBlackTurn;
            ui.updateBoard(board.getState(), isBlackTurn, lastMoveRow, lastMoveCol);
            ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore());

            // Check if next player can make a move
            if (!board.hasValidMoves(isBlackTurn)) {
                if (!board.hasValidMoves(!isBlackTurn)) {
                    endGame();
                } else {
                    isBlackTurn = !isBlackTurn;
                    if (gameMode != GameMode.AIvsAI) { // Only show message in human games
                        ui.showErrorMessage("No valid moves available. Turn skipped.");
                    }
                    ui.updateBoard(board.getState(), isBlackTurn, lastMoveRow, lastMoveCol);
                }
            }

            ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore());

            if (isAITurn()) {
                makeAIMove();
            }
        }
    }

    private void makeAIMove() {
        AI currentAI = isBlackTurn ? blackAI : whiteAI;
        if (currentAI != null) {
            Move move = currentAI.findBestMove(board, isBlackTurn);
            Timer timer = new Timer(100, _ -> { // 100 ms delay
                if (move != null) {
                    gameHistory.add(new MoveHistory(board, isBlackTurn, searchedNodes, placedTiles));
                    placedTiles++;
                    board.makeMove(move.row, move.col, isBlackTurn);
                    lastMoveRow = move.row;
                    lastMoveCol = move.col;
                    isBlackTurn = !isBlackTurn;
                    ui.updateBoard(board.getState(), isBlackTurn, lastMoveRow, lastMoveCol);
                    ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore());

                    // Check if next player can make a move
                    if (!board.hasValidMoves(isBlackTurn)) {
                        if (!board.hasValidMoves(!isBlackTurn)) {
                            endGame();
                        } else {
                            isBlackTurn = !isBlackTurn;
                            if (gameMode != GameMode.AIvsAI) { // Only show message in human games
                                ui.showErrorMessage("No valid moves available. Turn skipped.");
                            }
                            ui.updateBoard(board.getState(), isBlackTurn, lastMoveRow, lastMoveCol);
                        }
                    }

                    ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore());
                    if (isAITurn()) {
                        makeAIMove();
                    }
                }
            });
            timer.setRepeats(false); // Ensure the timer only runs once
            timer.start(); // Start the timer
        }
    }

    private boolean isAITurn() {
        return (gameMode == GameMode.AIvsAI) ||
                (gameMode == GameMode.HumanvsAI
                        && ((isBlackTurn && blackAI != null) || (!isBlackTurn && whiteAI != null)));
    }

    public void undoMove() {
        if (!gameHistory.isEmpty()) {
            MoveHistory lastState = gameHistory.removeLast();
            board = new Board(lastState.boardState);
            isBlackTurn = lastState.isBlackTurn;
            searchedNodes = lastState.searchedNodes;
            placedTiles = lastState.gameDepth;
            ui.updateBoard(board.getState(), isBlackTurn, -1, -1);
            ui.updateStatus(isBlackTurn ? "Black's turn" : "White's turn", getScore());
        }
    }

    private String getScore() {
        int blackCount = board.countPieces(1);
        int whiteCount = board.countPieces(2);
        String blackPlayer = getPlayerType(blackAI);
        String whitePlayer = getPlayerType(whiteAI);

        return String.format("Black (%s): %d  White (%s): %d", blackPlayer, blackCount, whitePlayer, whiteCount);
    }

    private void endGame() {
        int blackCount = board.countPieces(1);
        int whiteCount = board.countPieces(2);
        String winner = blackCount > whiteCount ? "Black" : whiteCount > blackCount ? "White" : "Tie";
        String player1 = getPlayerType(blackAI);
        String player2 = getPlayerType(whiteAI);
        GameLogger.logGameResult(gameMode.name(), player1, player2, gameStartTime, searchedNodes, blackCount,
                whiteCount, winner);
        ui.showEndGameDialog(winner, blackCount, whiteCount);
    }

    private String getPlayerType(AI ai) {
        if (gameMode == GameMode.AIvsAI) {
            return "AI (" + (ai == blackAI ? blackAI.difficulty : whiteAI.difficulty) + ")";
        } else if (gameMode == GameMode.HumanvsAI && ai != null && ai == whiteAI) {
            return "AI (" + whiteAI.difficulty + ")";
        } else if (gameMode == GameMode.HumanvsAI && ai != null && ai == blackAI) {
            return "AI (" + blackAI.difficulty + ")";
        } else {
            return "Human";
        }
    }
}