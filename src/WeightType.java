public class WeightType {
    // Fields representing the weights for different evaluation factors in the AI's decision-making process
    public int PieceDifferential; // Weight for the difference in the number of pieces between players
    public int Mobility;          // Weight for the number of valid moves available to a player
    public int Corner;            // Weight for controlling corner positions on the board
    public int Stability;         // Weight for the stability of pieces (how likely they are to be flipped)

    // Constructor to initialize the weights for the evaluation factors
    WeightType(int pieceDifferential, int mobility, int corner, int stability) {
        this.PieceDifferential = pieceDifferential; // Set the weight for piece differential
        this.Mobility = mobility;                  // Set the weight for mobility
        this.Corner = corner;                      // Set the weight for corner control
        this.Stability = stability;                // Set the weight for piece stability
    }
}