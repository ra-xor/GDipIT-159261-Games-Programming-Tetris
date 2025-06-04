import org.junit.Test;
import static org.junit.Assert.*;

public class TetrisTest {
    @Test
    public void testRowClearedAndScoreIncreased() {
        Tetris t = new Tetris();
        // fill top row completely
        for (int x = 0; x < t.WIDTH; x++) {
            t.grid[x][0] = 1;
        }
        int initialScore = t.playerScore;
        t.checkCompletedRows();
        for (int x = 0; x < t.WIDTH; x++) {
            assertEquals("Row should be cleared", 0, t.grid[x][0]);
        }
        assertTrue("Score should increase", t.playerScore > initialScore);
    }
}
