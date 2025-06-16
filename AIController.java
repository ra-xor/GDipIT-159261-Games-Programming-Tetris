public class AIController {

    private TetrisGame game;
    private int playerIndex;
    private Board board;
    private Piece currentPiece;

    // AI decision-making parameters (can be tuned)
    // Used for Simple evaluation: sum of cleared lines, negative for height, holes, bumpiness
    private static final double HEIGHT_WEIGHT = -0.510066;
    private static final double LINES_WEIGHT = 0.760666;
    private static final double HOLES_WEIGHT = -0.35663;
    private static final double BUMPINESS_WEIGHT = -0.184483;

    private double aiMoveTimer = 0;
    private double aiMoveDelay = 1; // Seconds between AI "thinking" and executing moves

    public AIController(TetrisGame game, int playerIndex) {
        this.game = game;
        this.playerIndex = playerIndex;
        // Board and piece will be updated each frame or when a new piece spawns
    }

    public void update(double dt) {
        if (game.getPlayerGameState(playerIndex) == null || game.getPlayerGameState(playerIndex).isGameOver() || game.isGloballyPaused()) {
            return;
        }
        if (game.getPlayerGameState(playerIndex).isShowCountdown()) { // Don't act during countdown
            return;
        }


        aiMoveTimer += dt;
        if (aiMoveTimer >= aiMoveDelay) {
            aiMoveTimer = 0;
            
            this.board = game.getBoard(playerIndex);
            this.currentPiece = game.getCurrentPiece(playerIndex);

            if (this.board == null || this.currentPiece == null) {
                return; // Not ready to make a move
            }
            
            BestMove bestMove = findBestMove();

            if (bestMove != null) {
                executeMove(bestMove);
            } else {
                // If no move found (should ideally not happen if piece is spawnable),
                // maybe just hard drop in current position as a fallback.
                game.hardDropActivePiece(playerIndex);
            }
        }
    }

    private class BestMove {
        int rotation;
        int xPosition;

        BestMove(int rotation, int xPosition) {
            this.rotation = rotation;
            this.xPosition = xPosition;
        }
    }

    private BestMove findBestMove() {
        BestMove bestMoveAction = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        Piece originalPiece = new Piece(this.currentPiece);

        for (int r = 0; r < 4; r++) {
            Piece simulatedPiece = new Piece(originalPiece);
            for(int i=0; i<r; i++) {
                 simulatedPiece.rotateClockwiseForSimulation();
            }
           
            for (int x = -simulatedPiece.getLeftmostX(); x < Board.WIDTH - simulatedPiece.getRightmostXAfterSpawn(x); x++) {
                Board simulatedBoard = new Board(this.board);
                Piece testPiece = new Piece(simulatedPiece);
                testPiece.setX(x);
                testPiece.setY(testPiece.getSpawnY());

                while (!testPiece.checkCollision(testPiece.getX(), testPiece.getY() + 1, testPiece.getCurrentRotation(), simulatedBoard)) {
                    testPiece.moveDownForSimulation();
                }
                
                simulatedBoard.addPieceToBoard(testPiece);

                double currentScore = evaluateBoard(simulatedBoard, testPiece);

                if (currentScore > bestScore) {
                    bestScore = currentScore;
                    bestMoveAction = new BestMove(r, x);
                }
            }
        }
        return bestMoveAction;
    }

    private double evaluateBoard(Board boardToEvaluate, Piece piecePlaced) {
        // Simple evaluation: sum of cleared lines, negative for height, holes, bumpiness
        int linesCleared = boardToEvaluate.countPotentialLineClears(piecePlaced);
        int aggregateHeight = boardToEvaluate.getAggregateHeight();
        int holes = boardToEvaluate.countHoles();
        int bumpiness = boardToEvaluate.getBumpiness();

        return linesCleared * LINES_WEIGHT +
               aggregateHeight * HEIGHT_WEIGHT +
               holes * HOLES_WEIGHT +
               bumpiness * BUMPINESS_WEIGHT;
    }
    

    private void executeMove(BestMove move) {
        // 1. Rotate piece to target rotation
        int currentRotation = currentPiece.getCurrentRotation();
        int rotationsNeeded = (move.rotation - currentRotation + 4) % 4;
        for (int i = 0; i < rotationsNeeded; i++) {
            game.rotateActivePiece(playerIndex, true); // Assuming clockwise rotation
        }

        // 2. Move piece to target xPosition
        int currentX = currentPiece.getX();
        if (move.xPosition < currentX) {
            for (int i = 0; i < currentX - move.xPosition; i++) {
                game.moveActivePieceLeft(playerIndex);
            }
        } else if (move.xPosition > currentX) {
            for (int i = 0; i < move.xPosition - currentX; i++) {
                game.moveActivePieceRight(playerIndex);
            }
        }
        // 3. Hard drop removed - game loop will handle soft drop
        // game.hardDropActivePiece(playerIndex); 
        // found a bug when using the soft drop, it would stop playing after a while.
    }
} 