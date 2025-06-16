import java.util.List;

public class Piece {
    private int pieceX;
    private int pieceY;
    private int pieceColor;
    private int shapeId;
    private int[][] activeShape;
    private Board board;
    private PieceGenerator pieceGenerator;
    private AssetManager assetManager;
    private int currentRotation; // Added to track rotation state for AI

    // Define the shapes of all tetriminoes (0=I, 1=J, 2=L, 3=O, 4=S, 5=T, 6=Z)
    public static final int[][][] SHAPES = {
            { { -1, 0 }, { 0, 0 }, { 1, 0 }, { 2, 0 } }, // I
            { { -1, 1 }, { 0, 1 }, { 1, 1 }, { 1, 0 } }, // J
            { { -1, 1 }, { 0, 1 }, { 1, 1 }, { -1, 0 } }, // L
            { { 0, 0 }, { 0, 1 }, { 1, 0 }, { 1, 1 } }, // O
            { { -1, 1 }, { 0, 1 }, { 0, 0 }, { 1, 0 } }, // S
            { { -1, 1 }, { 0, 1 }, { 1, 1 }, { 0, 0 } }, // T
            { { -1, 0 }, { 0, 0 }, { 0, 1 }, { 1, 1 } } // Z
    };

    // Constructor
    public Piece(Board board, PieceGenerator generator, AssetManager assetManager) {
        this.board = board;
        this.activeShape = new int[4][2];
        this.pieceGenerator = generator;
        this.assetManager = assetManager;
        this.currentRotation = 0; // Initialise rotation
    }

    // Copy constructor for simulation
    public Piece(Piece original) {
        this.pieceX = original.pieceX;
        this.pieceY = original.pieceY;
        this.pieceColor = original.pieceColor;
        this.shapeId = original.shapeId;
        this.activeShape = new int[4][2];
        for (int i = 0; i < 4; i++) {
            this.activeShape[i][0] = original.activeShape[i][0];
            this.activeShape[i][1] = original.activeShape[i][1];
        }
        this.board = original.board; // Should be the simulated board in AI context
        this.pieceGenerator = original.pieceGenerator; // Not strictly necessary for simulation if not spawning
        this.currentRotation = original.currentRotation;
    }

    public void spawnNewPiece() {
        this.shapeId = pieceGenerator.getNextPieceType();
        this.pieceColor = this.shapeId + 1;

        pieceY = Board.BUFFER_HEIGHT;
        pieceX = (Board.WIDTH / 2) - 1;
        this.currentRotation = 0; // Reset rotation on new piece

        for (int i = 0; i < 4; i++) {
            activeShape[i][0] = SHAPES[this.shapeId][i][0];
            activeShape[i][1] = SHAPES[this.shapeId][i][1];
        }

        if (shapeId == 0) { // I piece
            pieceY++;
        }
    }

    // New method to spawn a specific piece, bypassing the generator
    public void spawnSpecificPiece(int specificShapeId) {
        this.shapeId = specificShapeId;
        this.pieceColor = this.shapeId + 1;
        this.currentRotation = 0; // Reset rotation

        pieceY = Board.BUFFER_HEIGHT;
        pieceX = (Board.WIDTH / 2) - 1;

        for (int i = 0; i < 4; i++) {
            activeShape[i][0] = SHAPES[this.shapeId][i][0];
            activeShape[i][1] = SHAPES[this.shapeId][i][1];
        }

        if (this.shapeId == 0) {
            pieceY++;
        }
    }

    // Checks if the piece overlaps with the board or is out of bounds at its current spawn location
    public boolean checkSpawnCollision() {
        return !isValidPosition(activeShape, pieceX, pieceY);
    }

    // Add method to check for lock out condition
    public boolean isLockOut() {
        return board.isPieceInBufferZone(activeShape, pieceX, pieceY);
    }

    public boolean moveLeft() {
        if (canMove(-1, 0)) {
            pieceX--;
            assetManager.playSound(assetManager.moveSound);
            return true;
        }
        assetManager.playSound(assetManager.blockedMoveSound);
        return false;
    }

    public boolean moveRight() {
        if (canMove(1, 0)) {
            pieceX++;
            assetManager.playSound(assetManager.moveSound);
            return true;
        }
        assetManager.playSound(assetManager.blockedMoveSound);
        return false;
    }

    public boolean moveDown() {
        if (canMove(0, 1)) {
            pieceY++;
            return true;
        }
        return false;
    }

    public boolean isLanded() {
        return !canMove(0, 1);
    }

    private boolean canMove(int dx, int dy) {
        for (int[] block : activeShape) {
            int newX = pieceX + block[0] + dx;
            int newY = pieceY + block[1] + dy;

            if (!board.isWithinBounds(newX, newY) || board.isOccupied(newX, newY)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidPosition(int[][] shape, int newPieceX, int newPieceY) {
        for (int[] block : shape) {
            int x = newPieceX + block[0];
            int y = newPieceY + block[1];
            if (!board.isWithinBounds(x, y) || board.isOccupied(x, y)) {
                return false;
            }
        }
        return true;
    }

    public void lockPiece() {
        for (int[] block : activeShape) {
            board.placePiece(pieceX + block[0], pieceY + block[1], pieceColor);
        }
    }

    public int[][] getGhostCoordinates() {
        int ghostY = pieceY;
        while (true) {
            boolean willLand = false;
            for (int[] block : activeShape) {
                int x = pieceX + block[0];
                int y = ghostY + block[1] + 1;
                if (!board.isWithinBounds(x, y) || board.isOccupied(x, y)) {
                    willLand = true;
                    break;
                }
            }
            if (willLand)
                break;
            ghostY++;
        }

        int[][] ghostBlocks = new int[4][2];
        for (int i = 0; i < 4; i++) {
            ghostBlocks[i][0] = pieceX + activeShape[i][0];
            ghostBlocks[i][1] = ghostY + activeShape[i][1];
        }
        return ghostBlocks;
    }

    // Getters
    public int getX() {
        return pieceX;
    }

    public int getY() {
        return pieceY;
    }

    public void setY(int y) {
        this.pieceY = y;
    }

    public void setX(int x) {
        this.pieceX = x;
    }

    public int getColor() {
        return pieceColor;
    }

    public int[][] getShape() {
        return activeShape;
    }

    public int getCurrentRotation() { 
        return currentRotation; 
    }

    public boolean rotateClockwise() {
        if (this.shapeId == 3) // O piece cannot be rotated
            return false;

        int[][] candidate = new int[4][2];
        int pivotRelX = activeShape[1][0];
        int pivotRelY = activeShape[1][1];

        for (int i = 0; i < 4; i++) {
            int currentRelX = activeShape[i][0];
            int currentRelY = activeShape[i][1];

            int dx = currentRelX - pivotRelX;
            int dy = currentRelY - pivotRelY;

            int newRelDx = -dy;
            int newRelDy = dx;

            candidate[i][0] = pivotRelX + newRelDx;
            candidate[i][1] = pivotRelY + newRelDy;
        }

        if (isValidPosition(candidate, pieceX, pieceY)) {
            for (int i = 0; i < 4; i++) {
                activeShape[i][0] = candidate[i][0];
                activeShape[i][1] = candidate[i][1];
            }
            assetManager.playSound(assetManager.rotateSound);
            currentRotation = (currentRotation + 1) % 4; // Update rotation state
            return true;
        }
        assetManager.playSound(assetManager.blockedRotateSound);
        return false;
    }

    public boolean rotateCounterClockwise() {
        if (this.shapeId == 3)
            return false;

        int[][] candidate = new int[4][2];
        int pivotRelX = activeShape[1][0];
        int pivotRelY = activeShape[1][1];

        for (int i = 0; i < 4; i++) {
            int currentRelX = activeShape[i][0];
            int currentRelY = activeShape[i][1];

            int dx = currentRelX - pivotRelX;
            int dy = currentRelY - pivotRelY;

            int newRelDx = dy;
            int newRelDy = -dx;

            candidate[i][0] = pivotRelX + newRelDx;
            candidate[i][1] = pivotRelY + newRelDy;
        }

        if (isValidPosition(candidate, pieceX, pieceY)) {
            for (int i = 0; i < 4; i++) {
                activeShape[i][0] = candidate[i][0];
                activeShape[i][1] = candidate[i][1];
            }
            assetManager.playSound(assetManager.rotateSound);
            currentRotation = (currentRotation - 1 + 4) % 4; // Update rotation state
            return true;
        }
        assetManager.playSound(assetManager.blockedRotateSound);
        return false;
    }

    public List<Integer> getNextPieces() {
        return pieceGenerator.peekNextPieces();
    }

    public int getPieceType() {
        return this.shapeId;
    }

    // Simulation-specific rotation, no wall kicks, no game state changes
    public void rotateClockwiseForSimulation() {
        if (this.shapeId == 3) return; // O piece doesn\'t rotate

        int[][] newShape = new int[4][2];
        // Assuming pivot is the second block (index 1) as in game rotation
        // For shapes like 'I', this might need adjustment or a defined pivot per shape.
        // For simplicity, using the same logic as game rotation pivot.
        int pivotRelX = activeShape[1][0];
        int pivotRelY = activeShape[1][1];

        for (int i = 0; i < 4; i++) {
            int currentRelX = activeShape[i][0];
            int currentRelY = activeShape[i][1];

            int dx = currentRelX - pivotRelX;
            int dy = currentRelY - pivotRelY;

            // Rotate 90 degrees clockwise: (x, y) -> (-y, x) around pivot
            int newRelDx = -dy;
            int newRelDy = dx;

            newShape[i][0] = pivotRelX + newRelDx;
            newShape[i][1] = pivotRelY + newRelDy;
        }
        activeShape = newShape;
        currentRotation = (currentRotation + 1) % 4;
    }
    
    public void moveDownForSimulation() {
        this.pieceY++;
    }

    // Check collision for a simulated piece at a given position and rotation state
    public boolean checkCollision(int x, int y, int rotationState, Board simBoard) {
        // This method needs to use the shape corresponding to 'rotationState'
        // For now, it uses activeShape which is already rotated in the simulation loop.
        for (int[] block : activeShape) {
            int checkX = x + block[0];
            int checkY = y + block[1];
            if (!simBoard.isWithinBounds(checkX, checkY) || simBoard.isOccupied(checkX, checkY)) {
                return true;
            }
        }
        return false;
    }
    
    public int getSpawnY() {
        // Standard spawn Y. Adjust if I-piece specific logic from spawnNewPiece needs to be here.
        return Board.BUFFER_HEIGHT; 
    }

    public int getLeftmostX() {
        int minX = Integer.MAX_VALUE;
        for (int[] block : activeShape) {
            minX = Math.min(minX, block[0]);
        }
        return minX;
    }

    public int getRightmostXAfterSpawn(int currentPieceX) {
        // This method should calculate the rightmost extent of the piece
        // RELATIVE to the board, not just the piece's local coordinates.
        // It's used to determine valid spawn/drop columns.
        // The `currentPieceX` is the board column where the piece's origin is.
        int maxRelX = Integer.MIN_VALUE;
        for (int[] block : activeShape) {
            maxRelX = Math.max(maxRelX, block[0]);
        }
        // Returns the maximum extent from the piece's origin.
        // So, if a piece's origin is at `currentPieceX`, and its rightmost block is `maxRelX`
        // the actual board column is `currentPieceX + maxRelX`.
        // The loop in AIController `x < Board.WIDTH - simulatedPiece.getRightmostXAfterSpawn(x)`
        // seems to expect this to return the width or max relative X.
        // If `simulatedPiece.getRightmostXAfterSpawn(x)` returns `maxRelX`, then the condition becomes
        // `x < Board.WIDTH - maxRelX` which means `x + maxRelX < Board.WIDTH`. This is correct.
        return maxRelX;
    }
}
