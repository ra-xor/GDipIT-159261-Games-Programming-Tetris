import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class TetrisGame extends GameEngine {

    public static class Particle {
        double x, y; // current position (pixels)
        double vx, vy; // current velocity (pixels/sec)
        double lifetime; // how long this particle lives (seconds)
        double age; // how long it’s been alive (seconds)
        Color color; // particle color
        int size; // square size in pixels

        public Particle(double x, double y, double vx, double vy, double lifetime, Color color, int size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.lifetime = lifetime;
            this.age = 0;
            this.color = color;
            this.size = size;
        }

        public void update(double dt) {
            age += dt;

            x += vx * dt;
            y += vy * dt;
        }

        public boolean isDead() {
            return age >= lifetime;
        }

        public float alpha() {
            double ratio = Math.max(0, 1 - (age / lifetime));
            return (float) ratio;
        }
    }

    public static class RowFlash {
        int rowY; // cleared‐row index (board coords)
        double age; // how long it’s been alive
        double lifetime; // e.g. 0.12 seconds

        public RowFlash(int rowY) {
            this.rowY = rowY;
            this.age = 0;
            this.lifetime = 0.12; // you can adjust this for a slightly longer/shorter effect
        }

        public void update(double dt) {
            age += dt;
        }

        public boolean isDead() {
            return age >= lifetime;
        }

        // 0→1 fade curve, then 1→0 fade: use a simple triangle shape
        public float alpha() {
            double half = lifetime * 0.5;
            if (age < half) {
                // ramp up from 0→1 in first half
                return (float) (age / half);
            } else {
                // ramp down from 1→0 in second half
                return (float) (Math.max(0, 1 - ((age - half) / half)));
            }
        }

        // Compute a “height scale” 1.0→2.0 over lifetime (so it expands vertically)
        public double heightScale() {
            // linear from 1.0 up to 2.0 as age → lifetime
            double t = Math.min(1.0, age / lifetime);
            return 1.0 + t; // at t=0 →1.0, at t=1→2.0
        }
    }

    // 1.1b) Track active row‐flashes
    private List<RowFlash> rowFlashes = new ArrayList<>();

    // 3.1b) A list to hold all active particles
    private List<Particle> particles = new ArrayList<>();
    private Random particleRng = new Random();
    // private static final int NUM_PLAYERS = 2; // Replaced by activePlayers
    private int activePlayers = 0; // Number of players in the current game mode

    private Board[] boards;
    private Piece[] currentPieces;
    private ScoreManager[] scoreManagers;
    private GameState[] gameStates; // Still used for per-player game over, countdown, etc.
    private InputHandler[] inputHandlers;
    private Renderer renderer;
    private PieceGenerator pieceGenerator; // Shared piece generator
private AIController aiController; // Added AI Controller instance

    private GameState globalGameState; // Manages overall game mode, menu, global pause/help

    // Tile palette - order matters!
    private Color[] tileColors = {
            black, // 0 = empty cell
            cyan, // 1 = I piece (shapeId 0)
            blue, // 2 = J piece (shapeId 1)
            orange, // 3 = L piece (shapeId 2)
            yellow, // 4 = O piece (shapeId 3)
            green, // 5 = S piece (shapeId 4)
            purple, // 6 = T piece (shapeId 5)
            red, // 7 = Z piece (shapeId 6)
            new Color(100, 100, 100) // 8 = Garbage tile
    };

    // Per-player arrays - will be sized by activePlayers
    private double[] fallIntervals;
    private double[] fallTimers;
    private final double lockDelay = 0.5; // Shared lock delay
    private double[] lockTimers;
    private Integer[] heldPieceTypes;
    private boolean[] canHolds;

    // Global game running states (distinct from globalGameState.currentMode)
    private boolean isGloballyPaused = false;
    private boolean overallGameOver = false;
    private boolean gameHasStarted = false; // To know if game components are initialised
    private boolean isEscPaused = false; // New flag for ESC-initiated pause

    public static void main(String[] args) {
        createGame(new TetrisGame(), 30);
    }

    @Override
    public void init() {
        globalGameState = new GameState();
        setWindowSize(600, 470); // Changed from 420 to 470
        renderer = new Renderer(this, null, null, null, null, tileColors, globalGameState);
        gameHasStarted = false;
    }

    private void startGameMode(GameMode mode) {
        if (mode == GameMode.ONE_PLAYER) {
            activePlayers = 1;
            // Calculate width for 1 player: total player area + one spacing unit on each
            // side (or just padding)
            setWindowSize(Renderer.PLAYER_TOTAL_WIDTH + Renderer.BOARD_LEFT_PADDING * 2 + 100, 420);
        } else if (mode == GameMode.TWO_PLAYER) {
            activePlayers = 2;
            setWindowSize(Renderer.PLAYER_TOTAL_WIDTH * 2 + Renderer.PLAYER_AREA_SPACING
                    + Renderer.BOARD_LEFT_PADDING * 2 + 200, 420);
        } else if (mode == GameMode.ONE_PLAYER_VS_AI) { // New Mode
            activePlayers = 2; // Human (P0) vs AI (P1)
            setWindowSize(Renderer.PLAYER_TOTAL_WIDTH * 2 + Renderer.PLAYER_AREA_SPACING + Renderer.BOARD_LEFT_PADDING * 2, 420);
            aiController = new AIController(this, 1); // AI controls player 1
        } else {
            globalGameState.setCurrentMode(GameMode.MENU);
            setWindowSize(600, 470); // Changed from 420 to 470
            return;
        }

        // Initialise game components based on activePlayers
        pieceGenerator = new PieceGenerator(); // Fresh sequence for new game

        boards = new Board[activePlayers];
        currentPieces = new Piece[activePlayers];
        scoreManagers = new ScoreManager[activePlayers];
        gameStates = new GameState[activePlayers]; // Per-player states
        inputHandlers = new InputHandler[activePlayers];

        fallIntervals = new double[activePlayers];
        fallTimers = new double[activePlayers];
        lockTimers = new double[activePlayers];
        heldPieceTypes = new Integer[activePlayers];
        canHolds = new boolean[activePlayers];

        for (int i = 0; i < activePlayers; i++) {
                        boards[i] = new Board(renderer.getAssetManager());
            pieceGenerator = new PieceGenerator();
            currentPieces[i] = new Piece(boards[i], pieceGenerator, renderer.getAssetManager());
            scoreManagers[i] = new ScoreManager(boards[i]); // Pass board reference
            gameStates[i] = new GameState(); // Each player has their own game state (for game over, countdown etc)
                                             // but globalGameState.currentMode is the authority on overall mode
            gameStates[i].setCurrentMode(mode); // Set player's state to the active game mode
            inputHandlers[i] = new InputHandler(this, i);
        }

        // Update renderer with the now initialised game components
        renderer = new Renderer(this, boards, currentPieces, scoreManagers, gameStates, tileColors, globalGameState);

        globalGameState.setCurrentMode(mode);
        globalGameState.setShowHelp(false); // Ensure help is not shown when starting a game
        isGloballyPaused = false;
        overallGameOver = false;
        gameHasStarted = true;

        restartGame(); // Resets board, scores, spawns pieces for the selected mode

        // Start default BGM (track 1)
        renderer.getAssetManager().handleMusicInput('1');
    }

    public void resetFallTimer(int playerIndex) {
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers)
            return;
        this.fallTimers[playerIndex] = 0;
    }

    public void restartGame() { // Now resets based on activePlayers
        if (!gameHasStarted)
            return; // Don't restart if game components not set up

        overallGameOver = false;
        isGloballyPaused = false;

        for (int i = 0; i < activePlayers; i++) {
            if (gameStates[i] != null) {
                gameStates[i].reset(); // Reset individual player states
                gameStates[i].setCurrentMode(globalGameState.getCurrentMode()); // Ensure player state reflects current
                                                                                // game mode
                gameStates[i].setShowCountdown(true);
            }
            if (scoreManagers[i] != null)
                scoreManagers[i].reset();
            if (boards[i] != null)
                boards[i].clearBoard();
            if (inputHandlers[i] != null)
                inputHandlers[i].resetDAS();

            fallTimers[i] = 0;
            lockTimers[i] = 0;
            heldPieceTypes[i] = null;
            canHolds[i] = true;
            spawnNewPiece(i); // This might immediately set game over if spawn fails
            if (!gameStates[i].isGameOver()) { // Only update fall interval if not game over from spawn
                updateFallInterval(i);
            }
        }
        // Adjust window size based on active players after restart
        // This ensures if restartGame is called independently, window is correct.
        if (activePlayers == 1) {
            setWindowSize(Renderer.PLAYER_TOTAL_WIDTH + Renderer.BOARD_LEFT_PADDING * 2 + 100, 420);
        } else if (activePlayers == 2) {
            setWindowSize(Renderer.PLAYER_TOTAL_WIDTH * 2 + Renderer.PLAYER_AREA_SPACING
                    + Renderer.BOARD_LEFT_PADDING * 2 + 200, 420);
        } else {
            // This case should ideally not be reached if activePlayers is correctly managed
            // but as a fallback, set to menu size or a default.
            setWindowSize(600, 470); // Menu size
        }
    }

    private void spawnNewPiece(int playerIndex) {
        updateFallInterval(playerIndex);
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers)
            return;
        currentPieces[playerIndex].spawnNewPiece();
        if (currentPieces[playerIndex].checkSpawnCollision()) {
            gameStates[playerIndex].setGameOver(true);
            renderer.getAssetManager().playSound(renderer.getAssetManager().gameOverSound);
            renderer.getAssetManager().stopMusic();
            checkOverallGameOver();
        }
        fallTimers[playerIndex] = 0;
        lockTimers[playerIndex] = 0;
        canHolds[playerIndex] = true;
    }

    private void updateFallInterval(int playerIndex) {
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers)
            return;
        int level = scoreManagers[playerIndex].getLevel();
        double interval;
        switch (level) {
            case 1:
                interval = 48.0 / 60.0;
                break;
            case 2:
                interval = 43.0 / 60.0;
                break;
            case 3:
                interval = 38.0 / 60.0;
                break;
            case 4:
                interval = 33.0 / 60.0;
                break;
            case 5:
                interval = 28.0 / 60.0;
                break;
            case 6:
                interval = 23.0 / 60.0;
                break;
            case 7:
                interval = 18.0 / 60.0;
                break;
            case 8:
                interval = 13.0 / 60.0;
                break;
            case 9:
                interval = 8.0 / 60.0;
                break;
            case 10:
                interval = 6.0 / 60.0;
                break;
            case 11:
                interval = 5.0 / 60.0;
                break;
            case 12:
                interval = 4.0 / 60.0;
                break;
            case 13:
                interval = 3.0 / 60.0;
                break;
            case 14:
                interval = 2.0 / 60.0;
                break;
            case 15:
                interval = 1.0 / 60.0;
                break;
            default:
                interval = Math.max(1.0 / 60.0, 48.0 / 60.0 - ((level - 1) * 5.0 / 60.0));
        }
        fallIntervals[playerIndex] = interval;
    }

    private void checkOverallGameOver() {
        if (!gameHasStarted)
            return;
        int playersGameOver = 0;
        for (int i = 0; i < activePlayers; i++) {
            if (gameStates[i].isGameOver()) {
                playersGameOver++;
            }
        }
        // In 1P mode, any game over is overall game over.
        // In 2P mode, if one player is game over, the other wins (game might continue
        // for a moment or stop).
        // Or, if all but one player is game over.
        if (activePlayers > 0 && playersGameOver >= (activePlayers > 1 ? activePlayers - 1 : 1)) {
            if (activePlayers == 1 && playersGameOver == 1)
                overallGameOver = true;
            else if (activePlayers > 1 && playersGameOver >= activePlayers - 1) { // If one player left, or all topped
                                                                                  // out
                overallGameOver = true;
                // Could add logic to determine winner explicitly if needed for display
            }
        }
        // If only one player remains, this player is the winner, and the game over.
    }

    @Override
    public void update(double dt) {
        // 1.3a) Advance & remove dead particles
        Iterator<Particle> pit = particles.iterator();
        while (pit.hasNext()) {
            Particle p = pit.next();
            p.update(dt);
            if (p.isDead())
                pit.remove();
        }

        // 1.3b) Advance & remove expired row‐flashes
        Iterator<RowFlash> fit = rowFlashes.iterator();
        while (fit.hasNext()) {
            RowFlash f = fit.next();
            f.update(dt);
            if (f.isDead())
                fit.remove();
        }

        if (globalGameState.getCurrentMode() == GameMode.MENU || globalGameState.isShowHelp()) {
            // If in menu or help screen is shown globally, don't update game logic
            // Update countdown for any active player states if necessary (though usually
            // not in menu)
            if (gameHasStarted) { // Only if game components are initialised
                for (int i = 0; i < activePlayers; i++) {
                    if (gameStates[i] != null)
                        gameStates[i].updateCountdown();
                }
            }
            return;
        }

        if (!gameHasStarted || overallGameOver || isGloballyPaused) {
            // Update countdowns even if game is over or paused globally
            if (gameHasStarted) {
                for (int i = 0; i < activePlayers; i++) {
                    if (gameStates[i] != null)
                        gameStates[i].updateCountdown();
                }
            }
            return;
        }

        double maxDt = 0.1;
        if (dt > maxDt)
            dt = maxDt;

        for (int i = 0; i < activePlayers; i++) {
            if (inputHandlers[i] != null && 
                !(globalGameState.getCurrentMode() == GameMode.ONE_PLAYER_VS_AI && i == 1)) { // P0 gets input
                inputHandlers[i].update(dt);
            }
            if (gameStates[i] != null)
                gameStates[i].updateCountdown();

            if (gameStates[i].isGameOver() || gameStates[i].isPaused() || gameStates[i].isShowCountdown()) {
                continue;
            }

            // AI Controller Update for Player 1 in ONE_PLAYER_VS_AI mode
            if (globalGameState.getCurrentMode() == GameMode.ONE_PLAYER_VS_AI && i == 1 && aiController != null) {
                if (!gameStates[i].isGameOver() && !gameStates[i].isPaused() && !gameStates[i].isShowCountdown()) {
                    aiController.update(dt); // AI makes its move (rotates and positions)
                }
                // AI moves are discrete for rotation and horizontal. Fall logic below will handle soft drop.
                // We no longer 'continue' here, so the AI piece will go through the standard fall logic.
            }

            double currentFallSpeed = inputHandlers[i].isSoftDropping() ? (fallIntervals[i] / 20.0) : fallIntervals[i];
            // For AI player in ONE_PLAYER_VS_AI mode, force soft drop speed if AI is active for this player
            if (globalGameState.getCurrentMode() == GameMode.ONE_PLAYER_VS_AI && i == 1) {
                currentFallSpeed = fallIntervals[i] / 20.0; // Use soft drop speed for AI
            }
            
            fallTimers[i] += dt;

            while (fallTimers[i] >= currentFallSpeed) {
                fallTimers[i] -= currentFallSpeed;
                if (currentPieces[i].isLanded()) {
                    lockTimers[i] += currentFallSpeed;
                    if (lockTimers[i] >= lockDelay) {
                        lockPiece(i);
                        if (overallGameOver)
                            break; // If locking piece caused game over for all, stop.
                    }
                } else {
                    currentPieces[i].moveDown();
                    lockTimers[i] = 0;
                }
            }
            if (overallGameOver)
                break; // Break outer loop if game ended
        }

        // Update score popups
        for (int i = 0; i < activePlayers; i++) {
            if (boards[i] != null) {
                boards[i].updateScorePopups(dt);
            }
        }

        // Update background music
        if (!isGloballyPaused && gameHasStarted) {
            renderer.getAssetManager().updateMusic(dt);
        }
    }

    private void lockPiece(int playerIndex) {
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers
                || currentPieces == null || currentPieces[playerIndex] == null
                || boards == null || boards[playerIndex] == null
                || scoreManagers == null || scoreManagers[playerIndex] == null
                || gameStates == null || gameStates[playerIndex] == null) {
            return;
        }

        // 1) Lock the piece into the board
        currentPieces[playerIndex].lockPiece();
        renderer.getAssetManager().playSound(renderer.getAssetManager().lockSound);
        updateFallInterval(playerIndex);

        // 1. PRE‐SCAN visible rows for “full” before clearing
        List<Integer> rowsToFlash = new ArrayList<>();
        int[][] grid = boards[playerIndex].getGrid();
        for (int y = Board.BUFFER_HEIGHT; y < Board.TOTAL_HEIGHT; y++) {
            boolean rowFull = true;
            for (int x = 0; x < Board.WIDTH; x++) {
                if (grid[x][y] == 0) {
                    rowFull = false;
                    break;
                }
            }
            if (rowFull) {
                // record the board‐coordinate Y for this full row
                rowsToFlash.add(y);
            }
        }

        // 2) Now actually clear them (this shifts everything down internally)
        boards[playerIndex].checkAndClearCompletedRows(
                scoreManagers[playerIndex],
                scoreManagers[playerIndex].getLevel());

        int linesCleared = rowsToFlash.size();

        if (linesCleared > 0) {
            // 3) Determine on‐screen X‐offset of this player’s board
            int tilePx = Renderer.TILE_SIZE;
            int boardOffsetX;
            if (activePlayers == 1) {
                boardOffsetX = (this.mWidth - Board.WIDTH * tilePx) / 2;
            } else {
                int i = playerIndex;
                int baseOffset = i * (Renderer.PLAYER_TOTAL_WIDTH + Renderer.PLAYER_AREA_SPACING)
                        + Renderer.BOARD_LEFT_PADDING;
                boardOffsetX = (i == 0) ? baseOffset + 120 : baseOffset + 230;
            }

            // 4) For each pre‐shifted row Y, spawn both the RowFlash and a dense particle
            // cloud
            for (int boardY : rowsToFlash) {
                int displayY = boardY - Board.BUFFER_HEIGHT;
                if (displayY < 0)
                    continue;

                // (a) one RowFlash per cleared row
                rowFlashes.add(new RowFlash(displayY));

                // (b) now spawn a dense, medium‐speed, medium‐lifetime cloud
                double cellY = displayY * tilePx + (tilePx / 2.0);
                for (int xCell = 0; xCell < Board.WIDTH; xCell++) {
                    double cellX = boardOffsetX + xCell * tilePx + (tilePx / 2.0);

                    // use 80‒100 particles per cell for a solid look, but slower than before
                    int particlesPerCell = 300;
                    for (int k = 0; k < particlesPerCell; k++) {
                        // small jitter so they form a spreading blur
                        double jitter = tilePx * 0.1; // ≈2px
                        double px = cellX + (particleRng.nextDouble() * jitter * 2 - jitter);
                        double py = cellY + (particleRng.nextDouble() * jitter * 2 - jitter);

                        // moderate sideways speed so they visibly spread over ~0.1–0.2s
                        double vx = (particleRng.nextDouble() * 1200) - 600; // ±600 px/sec
                        double vy = 0;

                        // a bit longer lifetime so they spread visibly (0.10–0.15s)
                        double lifetime = 0.10 + particleRng.nextDouble() * 0.05;

                        // Jitter the inner spark color and outer glow color
                        if (particleRng.nextDouble() < 0.25) {
                            // 25% of particles are pure white “core sparks”
                            particles.add(new Particle(px, py, vx, vy, lifetime, Color.WHITE, 2));
                        } else {
                            // 75% are pale‐blue “outer sparks” with lower alpha
                            Color paleBlue = new Color(180, 220, 255, 180);
                            // use an RGBA with alpha=180 so these always draw semi‐transparent
                            particles.add(new Particle(px, py, vx, vy, lifetime, paleBlue, 2));
                        }

                    }
                }
            }
        }

        // 5) Send garbage (unchanged)
        if (linesCleared > 0 && activePlayers > 1) {
            int garbageToSend = 0;
            switch (linesCleared) {
                case 1:
                    garbageToSend = 0;
                    break;
                case 2:
                    garbageToSend = 1;
                    break;
                case 3:
                    garbageToSend = 2;
                    break;
                case 4:
                    garbageToSend = 4;
                    break;
                default:
                    garbageToSend = (linesCleared > 4)
                            ? (4 + (linesCleared - 4) * 2)
                            : 0;
            }
            if (garbageToSend > 0) {
                for (int opponentIndex = 0; opponentIndex < activePlayers; opponentIndex++) {
                    if (opponentIndex == playerIndex)
                        continue;
                    if (opponentIndex >= 0 && opponentIndex < activePlayers
                            && gameStates != null && opponentIndex < gameStates.length
                            && gameStates[opponentIndex] != null
                            && boards != null && opponentIndex < boards.length
                            && boards[opponentIndex] != null) {

                        if (!gameStates[opponentIndex].isGameOver()) {
                            boolean opponentTopped = boards[opponentIndex]
                                    .addGarbageLines(garbageToSend);
                            if (opponentTopped) {
                                gameStates[opponentIndex].setGameOver(true);
                                renderer.getAssetManager().playSound(renderer.getAssetManager().gameOverSound);
                                renderer.getAssetManager().stopMusic();
                                checkOverallGameOver(); 
                            }
                        }
                    }
                }
            }
        }

        // 6) Spawn next piece (unchanged)
        if (!overallGameOver) {
            spawnNewPiece(playerIndex);
        }
    }

    public void hardDropActivePiece(int playerIndex) {
        if (!gameHasStarted || currentPieces[playerIndex] == null || gameStates[playerIndex].isGameOver()
                || isGloballyPaused)
            return;
        while (!currentPieces[playerIndex].isLanded()) {
            currentPieces[playerIndex].moveDown();
        }
        renderer.getAssetManager().playSound(renderer.getAssetManager().hardDropSound);
        lockPiece(playerIndex);
    }

    public void rotateActivePiece(int playerIndex, boolean clockwise) {
        if (!gameHasStarted || currentPieces[playerIndex] == null || gameStates[playerIndex].isGameOver()
                || isGloballyPaused)
            return;
        if (clockwise) {
            currentPieces[playerIndex].rotateClockwise();
        } else {
            currentPieces[playerIndex].rotateCounterClockwise();
        }
        if (currentPieces[playerIndex].isLanded()) {
            lockTimers[playerIndex] = 0;
        }
    }

    public void holdActivePiece(int playerIndex) {
        if (!gameHasStarted || !canHolds[playerIndex] || currentPieces[playerIndex] == null
                || gameStates[playerIndex].isGameOver() || isGloballyPaused) {
            return;
        }
        int currentType = currentPieces[playerIndex].getPieceType();
        if (heldPieceTypes[playerIndex] == null) {
            heldPieceTypes[playerIndex] = currentType;
            spawnNewPiece(playerIndex);
        } else {
            int tempType = heldPieceTypes[playerIndex];
            heldPieceTypes[playerIndex] = currentType;
            currentPieces[playerIndex].spawnSpecificPiece(tempType);
            if (currentPieces[playerIndex].checkSpawnCollision()) {
                gameStates[playerIndex].setGameOver(true);
                renderer.getAssetManager().playSound(renderer.getAssetManager().gameOverSound);
                renderer.getAssetManager().stopMusic();
                checkOverallGameOver();
            }
        }
        renderer.getAssetManager().playSound(renderer.getAssetManager().holdSound);
        canHolds[playerIndex] = false;
        fallTimers[playerIndex] = 0;
        lockTimers[playerIndex] = 0;
    }

    public void moveActivePieceLeft(int playerIndex) {
        if (!gameHasStarted || currentPieces[playerIndex] == null || gameStates[playerIndex].isGameOver()
                || isGloballyPaused)
            return;
        currentPieces[playerIndex].moveLeft();
        if (currentPieces[playerIndex].isLanded()) {
            lockTimers[playerIndex] = 0;
        }
    }

    public void moveActivePieceRight(int playerIndex) {
        if (!gameHasStarted || currentPieces[playerIndex] == null || gameStates[playerIndex].isGameOver()
                || isGloballyPaused)
            return;
        currentPieces[playerIndex].moveRight();
        if (currentPieces[playerIndex].isLanded()) {
            lockTimers[playerIndex] = 0;
        }
    }

    @Override
    public void paintComponent() {
        if (renderer != null) {
            renderer.render();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (globalGameState.isShowHelp()) {
            if (keyCode == KeyEvent.VK_ESCAPE) {
                globalGameState.setShowHelp(false);
                // Return to appropriate window size
                if (gameHasStarted) {
                    if (activePlayers == 1) {
                        setWindowSize(Renderer.PLAYER_TOTAL_WIDTH + Renderer.BOARD_LEFT_PADDING * 2 + 100, 420);
                    } else {
                        setWindowSize(Renderer.PLAYER_TOTAL_WIDTH * 2 + Renderer.PLAYER_AREA_SPACING +
                                Renderer.BOARD_LEFT_PADDING * 2 + 200, 420);
                    }
                } else {
                    setWindowSize(600, 470); // Changed from 420 to 470 (menu size)
                }
            }
            return;
        }

        if (isGloballyPaused && isEscPaused) {
            switch (keyCode) {
                case KeyEvent.VK_ESCAPE:
                    isGloballyPaused = false;
                    isEscPaused = false;
                    renderer.getAssetManager().handleMusicInput('1');
                    for(int i=0; i<activePlayers; i++) {
                        if(gameStates[i] != null) gameStates[i].setShowCountdown(true);
                    }
                    break;
                case KeyEvent.VK_UP:
                    globalGameState.previousPauseOption();
                    break;
                case KeyEvent.VK_DOWN:
                    globalGameState.nextPauseOption();
                    break;
                case KeyEvent.VK_ENTER:
                    String selectedEscPauseOption = globalGameState.getSelectedPauseOption();
                    if ("Resume".equals(selectedEscPauseOption)) {
                        isGloballyPaused = false;
                        isEscPaused = false;
                        renderer.getAssetManager().handleMusicInput('1');
                        for(int i=0; i<activePlayers; i++) {
                            if(gameStates[i] != null) gameStates[i].setShowCountdown(true);
                        }
                    } else if ("Help".equals(selectedEscPauseOption)) {
                        globalGameState.setShowHelp(true);
                        setWindowSize(800, 450); // Changed from 600 to 500
                    } else if ("Quit".equals(selectedEscPauseOption)) {
                        globalGameState.setCurrentMode(GameMode.MENU);
                        isGloballyPaused = false;
                        isEscPaused = false;
                        gameHasStarted = false;
                        setWindowSize(600, 470); // Changed from 420 to 470
                    }
                    break;
            }
            return;
        }

        if (globalGameState.getCurrentMode() == GameMode.MENU) {
            switch (keyCode) {
                case KeyEvent.VK_UP:
                    globalGameState.previousGameModeOption();
                    break;
                case KeyEvent.VK_DOWN:
                    globalGameState.nextGameModeOption();
                    break;
                case KeyEvent.VK_ENTER:
                    String selectedOption = globalGameState.getSelectedGameModeOption();
                    if ("1 Player".equals(selectedOption)) {
                        startGameMode(GameMode.ONE_PLAYER);
                    } else if ("2 Players".equals(selectedOption)) {
                        startGameMode(GameMode.TWO_PLAYER);
                    } else if ("Player vs AI".equals(selectedOption)) { // New Menu Option
                        startGameMode(GameMode.ONE_PLAYER_VS_AI);
                    } else if ("Help".equals(selectedOption)) {
                        globalGameState.setShowHelp(true);
                        setWindowSize(800, 450); // Changed from 600 to 500
                    } else if ("Quit".equals(selectedOption)) {
                        System.exit(0);
                    }
                    break;
            }
            return;
        }

        // Gameplay Input (1P or 2P mode)
        // This block handles input if a game has been started (not in menu, not in
        // help, not in ESC pause)
        if (gameHasStarted) {
            if (overallGameOver) {
                if (keyCode == KeyEvent.VK_R) {
                    globalGameState.setCurrentMode(GameMode.MENU);
                    overallGameOver = false;
                    gameHasStarted = false;
                    setWindowSize(600, 470); // Reset to original menu window size
                }
                return; // Consume all other input when overall game over screen is shown
            }

            // If not overall game over, proceed with active game/pause logic:
            if (!isGloballyPaused) {
                for (int i = 0; i < activePlayers; i++) {
                    if (inputHandlers[i] != null && gameStates[i] != null && !gameStates[i].isGameOver()) {
                        // In AI mode, only player 0 gets keyboard input
                        if (globalGameState.getCurrentMode() == GameMode.ONE_PLAYER_VS_AI && i == 1) {
                            continue; 
                        }
                        inputHandlers[i].keyPressed(e);
                    }
                }
            }

            // Global Gameplay Keys during active play (when not overallGameOver)
            if (keyCode == KeyEvent.VK_P) {
                if (isGloballyPaused && isEscPaused) {
                    // P key does nothing if ESC pause is active
                } else {
                    isGloballyPaused = !isGloballyPaused;
                    isEscPaused = false;
                    if (isGloballyPaused) {
                        renderer.getAssetManager().stopMusic();
                    } else {
                        renderer.getAssetManager().handleMusicInput('1');
                        for (int i = 0; i < activePlayers; i++) {
                            if (gameStates[i] != null)
                                gameStates[i].setShowCountdown(true);
                        }
                    }
                }
            }

            if (keyCode == KeyEvent.VK_ESCAPE) {
                if (!isGloballyPaused) {
                    renderer.getAssetManager().stopMusic();
                    isGloballyPaused = true;
                    isEscPaused = true;
                    globalGameState.pauseMenuSelection = 0;
                }
                // If already globally paused (e.g., by P), ESC does nothing here;
                // ESC for P-pause is handled below, ESC for ESC-pause is handled at the top.
            }

            // Keys active when P-paused (NOT ESC-paused, NOT overallGameOver)
            if (isGloballyPaused && !isEscPaused) {
                if (keyCode == KeyEvent.VK_R) {
                    globalGameState.setCurrentMode(GameMode.MENU);
                    isGloballyPaused = false;
                    gameHasStarted = false;
                    setWindowSize(600, 470); // Changed from 420 to 470
                }
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    globalGameState.setCurrentMode(GameMode.MENU);
                    isGloballyPaused = false;
                    gameHasStarted = false;
                    setWindowSize(600, 470); // Changed from 420 to 470
                }
            }

            // Handle music controls only during gameplay
            if (gameHasStarted && !isGloballyPaused && !overallGameOver) {
                char key = e.getKeyChar();
                if (key >= '0' && key <= '3') {
                    renderer.getAssetManager().handleMusicInput(key);
                }
            }
            
            // Help option during gameplay (if implemented)
            if (keyCode == KeyEvent.VK_H) {
                globalGameState.setShowHelp(true);
                setWindowSize(800, 450); // Changed from 600 to 500
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (globalGameState.getCurrentMode() == GameMode.MENU || !gameHasStarted || globalGameState.isShowHelp()) {
            return; // No input release logic for menu or if game not running / help shown
        }
        for (int i = 0; i < activePlayers; i++) {
            if (inputHandlers[i] != null) {
                // In AI mode, only player 0 gets keyboard input release
                if (globalGameState.getCurrentMode() == GameMode.ONE_PLAYER_VS_AI && i == 1) {
                    continue;
                }
                inputHandlers[i].keyReleased(e);
            }
        }
    }

    // Getters
    public Board getBoard(int playerIndex) {
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers)
            return null;
        return boards[playerIndex];
    }

    public Piece getCurrentPiece(int playerIndex) {
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers)
            return null;
        return currentPieces[playerIndex];
    }

    public ScoreManager getScoreManager(int playerIndex) {
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers)
            return null;
        return scoreManagers[playerIndex];
    }

    // This returns the specific player's GameState (for individual game over,
    // countdown)
    public GameState getPlayerGameState(int playerIndex) {
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers)
            return null;
        return gameStates[playerIndex];
    }

    // This returns the global GameState (for menu, current mode)
    public GameState getGlobalGameState() {
        return globalGameState;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public Integer getHeldPieceType(int playerIndex) {
        if (!gameHasStarted || playerIndex < 0 || playerIndex >= activePlayers)
            return null;
        return heldPieceTypes[playerIndex];
    }

    public int getActivePlayers() {
        return activePlayers;
    } // Changed from getNumPlayers

    public Color[] getTileColors() {
        return tileColors;
    }

    public boolean isOverallGameOver() {
        return overallGameOver;
    }

    public boolean isGloballyPaused() {
        return isGloballyPaused;
    }

    public PieceGenerator getPieceGenerator() {
        return pieceGenerator;
    }

    public boolean hasGameStarted() {
        return gameHasStarted;
    }

    public boolean isEscPaused() {
        return isEscPaused;
    } // Getter for ESC pause state

    public List<?> getParticles() {
        return particles;
    } // Getter for particles list

    public List<?> getRowFlashes() {
        return rowFlashes;
    }
}