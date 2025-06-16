import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int WIDTH = 10;
    public static final int VISIBLE_HEIGHT = 20; // Standard visible playfield height
    public static final int BUFFER_HEIGHT = 20; // Buffer zone above visible area
    public static final int TOTAL_HEIGHT = VISIBLE_HEIGHT + BUFFER_HEIGHT;
    public static final int GARBAGE_TILE_ID = 8; // Identifier for garbage blocks

    private int[][] grid;
    private List<ScorePopup> scorePopups;
    private List<LevelUpPopup> levelUpPopups;
    private AssetManager assetManager;

    public Board(AssetManager assetManager) {
        grid = new int[WIDTH][TOTAL_HEIGHT];
        clearBoard();
        scorePopups = new ArrayList<>();
        levelUpPopups = new ArrayList<>();
        this.assetManager = assetManager;
    }

        // Copy constructor for simulation
    public Board(Board original) {
        this.grid = new int[WIDTH][TOTAL_HEIGHT];
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < TOTAL_HEIGHT; y++) {
                this.grid[x][y] = original.grid[x][y];
            }
        }
    }

    public void clearBoard() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < TOTAL_HEIGHT; y++) {
                grid[x][y] = 0;
            }
        }
    }

    // Method to check if a position is within bounds
    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < TOTAL_HEIGHT;
    }

    // Method to check if a cell is occupied
    public boolean isOccupied(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= TOTAL_HEIGHT) {
            return true;
        }
        return grid[x][y] != 0;
    }

    // Method to place a piece on the board (when it locks)
    public void placePiece(int x, int y, int color) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < TOTAL_HEIGHT) {
            grid[x][y] = color;
        }
    }

    public int checkAndClearCompletedRows(ScoreManager scoreManager, int currentLevel) {
        int linesCleared = 0;

        for (int y = TOTAL_HEIGHT - 1; y >= 0; y--) {
            boolean rowComplete = true;
            for (int x = 0; x < WIDTH; x++) {
                if (grid[x][y] == 0) {
                    rowComplete = false;
                    break;
                }
            }
            if (rowComplete) {
                linesCleared++;
                clearRow(y);
                y++; // re‐check this same index after shifting everything down
            }
        }
        if (linesCleared > 0) {
            // Play appropriate line clear sound
            switch(linesCleared) {
                case 1: assetManager.playSound(assetManager.singleLineSound); break;
                case 2: assetManager.playSound(assetManager.doubleLineSound); break;
                case 3: assetManager.playSound(assetManager.tripleLineSound); break;
                case 4: assetManager.playSound(assetManager.tetrisSound); break;
            }
            int scoreValue = ScoreManager.LINE_CLEAR_POINTS[linesCleared] * currentLevel;
            addScorePopup(linesCleared, scoreValue);
            scoreManager.addScoreForLines(linesCleared, currentLevel);
        }
        
        return linesCleared;
    }

    private void clearRow(int rowY) {
        for (int y = rowY; y > 0; y--) {
            for (int x = 0; x < WIDTH; x++) {
                grid[x][y] = grid[x][y - 1];
            }
        }
        for (int x = 0; x < WIDTH; x++) {
            grid[x][0] = 0;
        }
    }

    public int[][] getGrid() {
        return grid;
    }

    // Method to add a piece directly to the board grid for simulation purposes
    public void addPieceToBoard(Piece piece) {
        int pieceX = piece.getX();
        int pieceY = piece.getY();
        int pieceColor = piece.getColor();
        int[][] shape = piece.getShape();

        for (int[] block : shape) {
            int x = pieceX + block[0];
            int y = pieceY + block[1];
            if (isWithinBounds(x, y)) {
                grid[x][y] = pieceColor;
            }
        }
    }

    // Counts how many lines *would be* cleared if the piece was placed.
    // This version simulates the clear and counts, but does not alter the board state.
    public int countPotentialLineClears(Piece piecePlaced) {
        // This is a simplified version. A more accurate one would:
        // 1. Create a temporary board copy.
        // 2. Add the piece to the temp board.
        // 3. Run the actual clear line logic on the temp board and count.
        // For now, this just checks rows occupied by the piece blocks.
        // This is a placeholder and needs a more robust implementation for good AI.
        
        // For a quick estimate, let's check rows that the piece occupies
        boolean[] rowHasBlock = new boolean[TOTAL_HEIGHT];
        for(int[] block : piecePlaced.getShape()){
            int y = piecePlaced.getY() + block[1];
            if(y >=0 && y < TOTAL_HEIGHT) rowHasBlock[y] = true;
        }

        int linesCleared = 0;
        for (int y = 0; y < TOTAL_HEIGHT; y++) {
            if (!rowHasBlock[y]) continue; // Only check rows the piece might complete

            boolean rowComplete = true;
            for (int x = 0; x < WIDTH; x++) {
                 // Check if grid[x][y] is empty OR if it's part of the currently placed piece
                boolean partOfPlacedPiece = false;
                for(int[] block : piecePlaced.getShape()){ 
                    if (piecePlaced.getX() + block[0] == x && piecePlaced.getY() + block[1] == y) {
                        partOfPlacedPiece = true;
                        break;
                    }
                }
                if (grid[x][y] == 0 && !partOfPlacedPiece) { // If cell is empty AND not part of piece to be placed
                    rowComplete = false;
                    break;
                }
            }
            if (rowComplete) {
                linesCleared++;
            }
        }
        return linesCleared;
    }

    public int getAggregateHeight() {
        int totalHeight = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < TOTAL_HEIGHT; y++) {
                if (grid[x][y] != 0) {
                    totalHeight += (TOTAL_HEIGHT - y); // Add height from top
                    break; // Move to next column
                }
            }
        }
        return totalHeight;
    }

    public int countHoles() {
        int holes = 0;
        for (int x = 0; x < WIDTH; x++) {
            boolean blockFound = false;
            for (int y = 0; y < TOTAL_HEIGHT; y++) {
                if (grid[x][y] != 0) {
                    blockFound = true;
                } else if (blockFound && grid[x][y] == 0) {
                    holes++;
                }
            }
        }
        return holes;
    }

    public int getBumpiness() {
        int bumpiness = 0;
        int[] columnHeights = new int[WIDTH];
        for (int x = 0; x < WIDTH; x++) {
            columnHeights[x] = 0; // Default to max height (empty column)
            for (int y = 0; y < TOTAL_HEIGHT; y++) {
                if (grid[x][y] != 0) {
                    columnHeights[x] = TOTAL_HEIGHT - y;
                    break;
                }
            }
        }

        for (int x = 0; x < WIDTH - 1; x++) {
            bumpiness += Math.abs(columnHeights[x] - columnHeights[x+1]);
        }
        return bumpiness;
    }

    // Method to add garbage lines at the bottom of the board
    // Returns true if adding lines results in a top-out (game over for this board)
    public boolean addGarbageLines(int numLinesToAdd) {
        if (numLinesToAdd <= 0) return false;

        int effectiveNumLinesToAdd = Math.min(numLinesToAdd, TOTAL_HEIGHT);

        for (int y = 0; y < TOTAL_HEIGHT - effectiveNumLinesToAdd; y++) {
            for (int x = 0; x < WIDTH; x++) {
                grid[x][y] = grid[x][y + effectiveNumLinesToAdd];
                if (grid[x][y] != 0 && y < BUFFER_HEIGHT) {
                    return true;
                }
            }
        }

        for (int lineY = TOTAL_HEIGHT - effectiveNumLinesToAdd; lineY < TOTAL_HEIGHT; lineY++) {
            int holeX = (int) (Math.random() * WIDTH);
            for (int x = 0; x < WIDTH; x++) {
                if (x == holeX) {
                    grid[x][lineY] = 0;
                } else {
                    grid[x][lineY] = GARBAGE_TILE_ID;
                }
            }
        }
        return false;
    }

    // Add method to check if a piece is entirely in buffer zone
    public boolean isPieceInBufferZone(int[][] shape, int pieceX, int pieceY) {
        for (int[] block : shape) {
            int y = pieceY + block[1];
            if (y >= BUFFER_HEIGHT) {
                return false;
            }
        }
        return true;
    }
public void updateScorePopups(double dt) {
        scorePopups.removeIf(popup -> !popup.update(dt));
        levelUpPopups.removeIf(popup -> !popup.update(dt));
    }

    public void onLevelUp(int newLevel) {
        assetManager.playSound(assetManager.levelUpSound);
        // Create popup at middle of the board
        int popupY = (VISIBLE_HEIGHT / 2) * Renderer.TILE_SIZE;
        levelUpPopups.add(new LevelUpPopup(newLevel, popupY));
    }

    public List<ScorePopup> getScorePopups() {
        return scorePopups;
    }

    public List<LevelUpPopup> getLevelUpPopups() {
        return levelUpPopups;
    }

    public void addScorePopup(int linesCleared, int scoreValue) {
        String type = "";
        switch(linesCleared) {
            case 1: type = "Single"; break;
            case 2: type = "Double"; break;
            case 3: type = "Triple"; break;
            case 4: type = "Tetris!"; break;
        }
        int popupY = (int)(VISIBLE_HEIGHT * 0.75 * Renderer.TILE_SIZE);
        scorePopups.add(new ScorePopup(type, scoreValue, popupY));
    }
}
