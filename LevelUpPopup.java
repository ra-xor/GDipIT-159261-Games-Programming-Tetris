public class LevelUpPopup {
    private int level;
    private float y;
    private float scale;
    private float alpha;
    private static final float RISE_SPEED = 80.0f;      // Pixels per second
    private static final float FALL_SPEED = 120.0f;     // Pixels per second
    private static final float SCALE_SPEED = 1.5f;      // Scale change per second
    private static final float MAX_SCALE = 2.0f;        // Maximum size multiplier
    private static final float MIN_SCALE = 0.5f;        // Minimum size multiplier
    private boolean isFalling;
    private float lifetime;

    public LevelUpPopup(int level, int startY) {
        this.level = level;
        this.y = startY;
        this.scale = 1.0f;
        this.alpha = 1.0f;
        this.isFalling = false;
        this.lifetime = 0;
    }

    public boolean update(double dt) {
        lifetime += dt;
        
        if (!isFalling) {
            // Rising and growing phase
            y -= RISE_SPEED * dt;
            scale = (float)Math.min(MAX_SCALE, scale + SCALE_SPEED * dt);
            
            if (lifetime > 1.0) {
                isFalling = true;
            }
        } else {
            // Falling and shrinking phase
            y += FALL_SPEED * dt;
            scale = (float)Math.max(MIN_SCALE, scale - SCALE_SPEED * dt);
            alpha -= dt;
        }
        
        return alpha > 0;
    }

    // Getters
    public int getLevel() { return level; }
    public float getY() { return y; }
    public float getScale() { return scale; }
    public float getAlpha() { return alpha; }
}