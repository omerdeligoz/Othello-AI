public class WeightType {
    public int PieceDifferential;
    public int Mobility;
    public int Corner;
    public int Stability;

    WeightType(int pieceDifferential, int mobility, int corner, int stability) {
        this.PieceDifferential = pieceDifferential;
        this.Mobility = mobility;
        this.Corner = corner;
        this.Stability = stability;
    }
}