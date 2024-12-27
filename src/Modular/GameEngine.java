package Modular;

public class GameEngine {
    private Board board;
    private AI blackAI;
    private AI whiteAI;
    private boolean isBlackTurn;
    private Reversi ui;
    private GameMode gameMode;
    private int lastMoveRow = -1;
    private int lastMoveCol = -1;
    private long gameStartTime;

    public GameEngine(Reversi ui, GameMode gameMode, AIDifficulty blackAIDifficulty, AIDifficulty whiteAIDifficulty) {
        this.ui = ui;
        this.gameMode = gameMode;
        this.board = new Board();
        this.isBlackTurn = true;

        // Initialize AI players based on game mode
        if (gameMode == GameMode.HumanvsAI || gameMode == GameMode.AIvsAI) {
            this.blackAI = (blackAIDifficulty != null) ? new AI(blackAIDifficulty) : null;
            this.whiteAI = (whiteAIDifficulty != null) ? new AI(whiteAIDifficulty) : null;
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
            if (move != null) {
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
        }
    }

    private boolean isAITurn() {
        return (gameMode == GameMode.AIvsAI) ||
                (gameMode == GameMode.HumanvsAI
                        && ((isBlackTurn && blackAI != null) || (!isBlackTurn && whiteAI != null)));
    }

    private String getScore() {
        int blackCount = board.countPieces(1);
        int whiteCount = board.countPieces(2);
        int emptyCount = Board.BOARD_SIZE * Board.BOARD_SIZE - (blackCount + whiteCount);
        String blackPlayer = getPlayerType(blackAI);
        String whitePlayer = getPlayerType(whiteAI);

        return String.format("Black (%s): %d  White (%s): %d  Empty: %d", blackPlayer, blackCount, whitePlayer,
                whiteCount, emptyCount);
    }

    private void endGame() {
        int blackCount = board.countPieces(1);
        int whiteCount = board.countPieces(2);
        String winner = blackCount > whiteCount ? "Black" : whiteCount > blackCount ? "White" : "Tie";
        String player1 = getPlayerType(blackAI);
        String player2 = getPlayerType(whiteAI);
        GameLogger.logGameResult(gameMode.name(), player1, player2, gameStartTime, AI.searchedNodes, blackCount,
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