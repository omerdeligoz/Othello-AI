package Modular;

public class Player {
    private boolean isBlack;
    private PlayerType type;

    public Player(boolean isBlack, PlayerType type) {
        this.isBlack = isBlack;
        this.type = type;
    }

    public boolean isBlack() {
        return isBlack;
    }

    public PlayerType getType() {
        return type;
    }
}

enum PlayerType {
    HUMAN, AI
}