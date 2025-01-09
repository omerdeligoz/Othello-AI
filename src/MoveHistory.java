public class MoveHistory {
    Board boardState;
    boolean isBlackTurn;
    int searchedNodes;
    int gameDepth;

    MoveHistory(Board board, boolean turn, int nodes, int depth) {
        this.boardState = new Board(board);
        this.isBlackTurn = turn;
        this.searchedNodes = nodes;
        this.gameDepth = depth;
    }
}