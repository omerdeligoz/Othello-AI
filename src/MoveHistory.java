public class MoveHistory {
    Board boardState; // Represents the state of the board at the time of the move
    boolean isBlackTurn; // Indicates whose turn it was (true for black, false for white)
    int searchedNodes; // Number of nodes searched by the AI up to this point
    int gameDepth; // Represents the depth of the game (number of moves made so far)
    Move lastMove; // The last move made by the player

    // Constructor to initialize a MoveHistory object
    MoveHistory(Board board, boolean turn, int nodes, int depth, Move lastMove) {
        this.boardState = new Board(board); // Create a deep copy of the board state
        this.isBlackTurn = turn; // Store whose turn it was
        this.searchedNodes = nodes; // Store the number of nodes searched by the AI
        this.gameDepth = depth; // Store the depth of the game (number of moves made)
        this.lastMove = lastMove; // Store the last move made by the player
    }
}