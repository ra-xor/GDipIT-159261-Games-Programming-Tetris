public class ScorePopup {
    private String scoreType;
    private int scoreValue;
    private float y;
    private float alpha;
    private static final float MOVE_SPEED = 60.0f; // Pixels per second
    private static final float FADE_SPEED = 1.2f;  // Alpha decrease per second
  

    public ScorePopup(String type, int value, int startY) {
        this.scoreType = type;
        this.scoreValue = value;
        this.y = startY;
        this.alpha = 1.0f;
    }

    public boolean update(double dt) {
        y -= MOVE_SPEED * dt;  // Move upward
        alpha -= FADE_SPEED * dt;  // Fade out
        return alpha > 0;  // Return false when fully faded
    }

    // Getters
    public String getScoreType() { return scoreType; }
    public int getScoreValue() { return scoreValue; }
    public float getY() { return y; }
    public float getAlpha() { return alpha; }
}