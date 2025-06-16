import java.awt.Color;
import java.awt.FontMetrics;
import java.util.List;
import java.util.ArrayList;

public class Renderer {
    private TetrisGame game; // TetrisGame instance to access game-wide properties like numPlayers
    private GameEngine ge; // To access drawing methods from GameEngine (via TetrisGame)
    private Board[] boards;
    private Piece[] currentPieces;
    private ScoreManager[] scoreManagers;
    private GameState[] gameStates; // Per-player game states
    private GameState globalGameState; // For menu, current mode, global help
    private AssetManager assetManager;

    private Color[] tileColors;

    // Visual layout constants
    public static final int TILE_SIZE = 20;
    public static final int BOARD_LEFT_PADDING = 1 * TILE_SIZE; // Padding for the board within a player's area
    // Width of the visual board area including its own left/right walls
    public static final int PLAYER_BOARD_VISUAL_WIDTH = (Board.WIDTH + 2) * TILE_SIZE;
    public static final int INFO_PANEL_WIDTH = 110; // Width for score, next, hold piece
    public static final int PLAYER_AREA_SPACING = 30; // Horizontal space between player areas
    // Total width allocated for one player's display (board + info panel)
    public static final int PLAYER_TOTAL_WIDTH = PLAYER_BOARD_VISUAL_WIDTH + INFO_PANEL_WIDTH;

    // Constants for Next/Hold piece display, relative to their info panel X
    // position
    private static final int PREVIEW_PIECE_SIZE = 15;
    private static final int PREVIEW_SPACING_Y = 50; // Vertical space between previews

    public Renderer(TetrisGame game, Board[] boards, Piece[] pieces,
            ScoreManager[] scoreManagers, GameState[] gameStates,
            Color[] tileColors, GameState globalGameState) {
        this.game = game;
        this.ge = game; // GameEngine methods are available through TetrisGame
        this.boards = boards;
        this.currentPieces = pieces;
        this.scoreManagers = scoreManagers;
        this.gameStates = gameStates; // Array of player-specific states
        this.tileColors = tileColors;
        this.globalGameState = globalGameState; // Overall game state/menu manager
        this.assetManager = new AssetManager(ge); // Initialise AssetManager here
        this.assetManager.loadAll(); // Load assets once
    }

    // Constructor for menu/initial state before full game components are ready
    public Renderer(TetrisGame game, Color[] tileColors, GameState globalGameState) {
        this.game = game;
        this.ge = game;
        this.tileColors = tileColors;
        this.globalGameState = globalGameState;
        this.assetManager = new AssetManager(ge);
        this.assetManager.loadAll();
        // boards, currentPieces, scoreManagers, gameStates (array) will be null
        // initially
    }

    public void render() {
        ge.mFrame.setTitle("TETRIS");
        ge.drawImage(assetManager.Background, 0, 0, ge.mWidth, ge.mHeight);

        if (globalGameState == null)
            return;

        if (globalGameState.isShowHelp()) {
            drawHelpScreen();
            return;
        }

        // If game is over, we don't want to draw the menu until the game over screen is acknowledged (e.g. by pressing R)
        if (globalGameState.getCurrentMode() == GameMode.MENU && (game == null || !game.isOverallGameOver())) {
            drawGameModeMenu();
            return;
        }

        if (game != null && game.hasGameStarted()) {
            if (game.isGloballyPaused()) { 
                // Draw boards first, then pause message on top
                for (int i = 0; i < game.getActivePlayers(); i++) {
                    drawPlayerArea(i); 
                }
                if (game.isEscPaused()) {
                    drawEscPauseMenu();
                } else {
                    drawGlobalPauseScreen();
                }
                return;
            }
            if (game.isOverallGameOver()) {
                // For 2-player or PvAI, draw boards THEN the game over screen
                // For 1-player, original behavior (just game over screen) is fine, but drawing board under is also okay.
                for (int i = 0; i < game.getActivePlayers(); i++) {
                     drawPlayerArea(i); // Draw each player's board and info
                }
                drawOverallGameOverScreen(); // Then draw the game over message on top
                return;
            }
        } else {
            if (globalGameState.getCurrentMode() == GameMode.MENU) {
                drawGameModeMenu(); // Show menu if game isn't running
            }
            return; 
        }



        if (game.getActivePlayers() == 2) {
            int midX = ge.mWidth / 2;
            ge.changeColor(ge.white);
            ge.drawLine(midX, 0, midX, ge.mHeight, 6);
        }



        for (int i = 0; i < game.getActivePlayers(); i++) {
            int playerAreaOffsetX = i * (PLAYER_TOTAL_WIDTH + PLAYER_AREA_SPACING) + BOARD_LEFT_PADDING;
            int boardRenderOffsetX = playerAreaOffsetX;

            // 1) Shifting in 2P mode
            if (game.getActivePlayers() == 2) {
                if (i == 0) {
                    boardRenderOffsetX += 120;
                } else {
                    boardRenderOffsetX += 230;
                }
            }
            // 2) Center in 1P mode
            if (game.getActivePlayers() == 1) {
                boardRenderOffsetX = (ge.mWidth - Board.WIDTH * TILE_SIZE) / 2;
            }

            // 3) Draw board background, grid, walls, pieces (unchanged)
            ge.changeColor(ge.black);
            ge.drawSolidRectangle(boardRenderOffsetX, 0,
                    Board.WIDTH * TILE_SIZE,
                    Board.VISIBLE_HEIGHT * TILE_SIZE);
            drawGridLines(i, boardRenderOffsetX);

            int borderOffsetX;
            if (game.getActivePlayers() == 1) {
                borderOffsetX = boardRenderOffsetX - TILE_SIZE;
            } else {
                int baseWallX = playerAreaOffsetX - BOARD_LEFT_PADDING;
                if (i == 0)
                    borderOffsetX = baseWallX + 120;
                else
                    borderOffsetX = baseWallX + 230;
            }

            drawBorderWalls(i, borderOffsetX);
            drawPlacedTiles(i, boardRenderOffsetX);

            // 2.2a) First draw any row‐flashes 
            drawRowFlashes(boardRenderOffsetX);

            // 2.2b) Then draw the sideways particles 
            drawParticles(boardRenderOffsetX);

            if (!gameStates[i].isShowCountdown() &&
                    !gameStates[i].isGameOver() &&
                    !gameStates[i].isPaused()) {
                if (currentPieces != null && currentPieces[i] != null) {
                    drawGhostPiece(i, boardRenderOffsetX);
                    drawCurrentPiece(i, boardRenderOffsetX);
                }
            }

            if (gameStates[i].isShowCountdown()) {
                drawCountdown(i, boardRenderOffsetX, PLAYER_BOARD_VISUAL_WIDTH - 2 * TILE_SIZE);
            } else if (gameStates[i].isGameOver()) {
                drawPlayerGameOverScreen(i, boardRenderOffsetX, PLAYER_BOARD_VISUAL_WIDTH - 2 * TILE_SIZE);
            } else if (gameStates[i].isPaused()) {
                drawPlayerPauseMenu(i, boardRenderOffsetX, PLAYER_BOARD_VISUAL_WIDTH - 2 * TILE_SIZE);
            }

            // 4) Manually compute (x,y) for Score/Level 
            // Example: bottom-left of the board, with 30px margin
            int scoreX = boardRenderOffsetX - 115;
            int scoreY = ge.mHeight - 100; // pick “base Y” near the bottom (adjust as needed)

            drawScoreAndLevel(i, scoreX, scoreY);

            // 5) Manually compute (x,y) for Hold HUD 
            // Example: top-left of the board, 30px from top border
            int holdX = boardRenderOffsetX - 115;
            int holdY = 80; // pick Y=50 as “30px below top” (adjust as needed)

            drawHoldPiece(i, holdX, holdY);

            // 6) Manually compute (x,y) for Next HUD 
            // Example: right edge of board, 30px from top
            int nextX = boardRenderOffsetX + (Board.WIDTH * TILE_SIZE) + 50;
            int nextY = 130; // same 30px from top (adjust as needed)

            drawNextPieces(i, nextX, nextY);

            drawPopups(i, boardRenderOffsetX); 
        }
    }

    // Helper method to draw a player's full game area
    private void drawPlayerArea(int playerIndex) {
        if (game == null || !game.hasGameStarted()) return; // Should not happen if called from valid context

        // Calculate offsets for the current player
        int playerAreaBaseX = BOARD_LEFT_PADDING; // Base padding for the first player or standalone elements
        int playerWidthAllocation = PLAYER_TOTAL_WIDTH + PLAYER_AREA_SPACING;
        int playerAreaOffsetX = playerAreaBaseX + (playerIndex * playerWidthAllocation);
        int boardRenderOffsetX = playerAreaOffsetX; 
        int infoPanelX = playerAreaOffsetX + PLAYER_BOARD_VISUAL_WIDTH - TILE_SIZE + TILE_SIZE / 2; 

        // Check if necessary components for this player exist
        boolean playerComponentsExist = boards != null && playerIndex < boards.length && boards[playerIndex] != null &&
                                      gameStates != null && playerIndex < gameStates.length && gameStates[playerIndex] != null &&
                                      scoreManagers != null && playerIndex < scoreManagers.length && scoreManagers[playerIndex] != null;

        if (!playerComponentsExist) {
            // If components for this specific player don't exist (e.g. mid-initialization or error),
            // we might skip drawing this player or draw a placeholder. For now, skip.
            return;
        }

        drawGridLines(playerIndex, boardRenderOffsetX); // Pass playerIndex if needed, or just offset
        drawBorderWalls(playerIndex, boardRenderOffsetX - TILE_SIZE); // Wall is one TILE_SIZE left of board cells
        drawPlacedTiles(playerIndex, boardRenderOffsetX);

        // Only draw dynamic elements if the overall game is not finished.
        // If game is over, we just want the static board and player info.
        if (!game.isOverallGameOver()) {
            if (gameStates[playerIndex].isShowCountdown()) {
                drawCountdown(playerIndex, boardRenderOffsetX, PLAYER_BOARD_VISUAL_WIDTH - 2 * TILE_SIZE);
            } else if (gameStates[playerIndex].isGameOver()) { // Player-specific game over (e.g., one player topped out in 2P)
                drawPlayerGameOverScreen(playerIndex, boardRenderOffsetX, PLAYER_BOARD_VISUAL_WIDTH - 2 * TILE_SIZE);
            } else if (gameStates[playerIndex].isPaused()) { 
                // This would be for an individual player pause, if implemented. Global pause is handled above.
                drawPlayerPauseMenu(playerIndex, boardRenderOffsetX, PLAYER_BOARD_VISUAL_WIDTH - 2 * TILE_SIZE);
            } else {
                // Active piece and ghost piece for players still playing
                if (currentPieces != null && playerIndex < currentPieces.length && currentPieces[playerIndex] != null) {
                    drawGhostPiece(playerIndex, boardRenderOffsetX);
                    drawCurrentPiece(playerIndex, boardRenderOffsetX);
                }
            }
        }
        
        drawPlayerInfo(playerIndex, infoPanelX); // Score, level, next, hold - always show these
    }

    // Draws the player's info panel: score, level, hold, and next pieces
    private void drawPlayerInfo(int playerIndex, int infoPanelX) {
        // Score and Level
        int scoreY = ge.mHeight - 100;
        drawScoreAndLevel(playerIndex, infoPanelX - 115, scoreY);

        // Hold piece
        int holdY = 80;
        drawHoldPiece(playerIndex, infoPanelX - 115, holdY);

        // Next pieces
        int nextY = 130;
        drawNextPieces(playerIndex, infoPanelX + (Board.WIDTH * TILE_SIZE) + 50 - infoPanelX, nextY);
    }

    private void drawGridLines(int playerIndex, int boardOffsetX) {
        ge.changeColor(50, 50, 50);
        // Use Board class constants for dimensions as boards[playerIndex] might be null
        // initially
        for (int x = 0; x <= Board.WIDTH; x++) {
            int px = boardOffsetX + x * TILE_SIZE;
            ge.drawLine(px, 0, px, Board.VISIBLE_HEIGHT * TILE_SIZE);
        }
        for (int y = 0; y <= Board.VISIBLE_HEIGHT; y++) {
            int py = y * TILE_SIZE;
            ge.drawLine(boardOffsetX, py, boardOffsetX + Board.WIDTH * TILE_SIZE, py);
        }
    }

    private void drawBorderWalls(int playerIndex, int playerAreaOffsetX) {
        ge.changeColor(ge.white);

        if (game.getActivePlayers() == 1) {
            // board left edge is centered at (windowWidth - boardWidth) / 2
            int boardLeftX = (ge.mWidth - Board.WIDTH * TILE_SIZE) / 2;
            // walls sit one TILE_SIZE to the left of that
            playerAreaOffsetX = boardLeftX - TILE_SIZE;
        }
        int boardVisibleHeightPx = Board.VISIBLE_HEIGHT * TILE_SIZE;
        // int boardTotalWidthPx = Board.WIDTH * TILE_SIZE;

        // Left wall - at playerAreaOffsetX
        for (int yCell = 0; yCell < Board.VISIBLE_HEIGHT; yCell++) {
            ge.drawSolidRectangle(playerAreaOffsetX, yCell * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
        // Right wall - at playerAreaOffsetX + (Board.WIDTH+1)*TILE_SIZE
        int rightWallX = playerAreaOffsetX + (Board.WIDTH + 1) * TILE_SIZE;
        for (int yCell = 0; yCell < Board.VISIBLE_HEIGHT; yCell++) {
            ge.drawSolidRectangle(rightWallX, yCell * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
        // Bottom wall
        for (int xCell = 0; xCell < Board.WIDTH + 2; xCell++) { // +2 to include side wall positions
            ge.drawSolidRectangle(playerAreaOffsetX + xCell * TILE_SIZE, boardVisibleHeightPx, TILE_SIZE, TILE_SIZE);
        }
    }

    private void drawPlacedTiles(int playerIndex, int boardOffsetX) {
        if (boards == null || boards[playerIndex] == null)
            return;
        int[][] grid = boards[playerIndex].getGrid();
        for (int y = Board.BUFFER_HEIGHT; y < Board.TOTAL_HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                if (grid[x][y] != 0) {
                    ge.changeColor(tileColors[grid[x][y]]);
                    int displayY = y - Board.BUFFER_HEIGHT;
                    ge.drawImage(assetManager.tilePalette[grid[x][y]],
                            boardOffsetX + x * TILE_SIZE + 1,
                            displayY * TILE_SIZE + 1,
                            TILE_SIZE - 2, TILE_SIZE - 2);
                }
            }
        }
    }

    private void drawCurrentPiece(int playerIndex, int boardOffsetX) {
        if (currentPieces == null || currentPieces[playerIndex] == null || boards == null
                || boards[playerIndex] == null)
            return;

        int pieceColorIndex = currentPieces[playerIndex].getColor();
        ge.changeColor(tileColors[pieceColorIndex]); // Set color for potential fallback if image fails
        int[][] shape = currentPieces[playerIndex].getShape();

        for (int[] block : shape) {
            int px = currentPieces[playerIndex].getX() + block[0];
            int py = currentPieces[playerIndex].getY() + block[1];
            if (py >= Board.BUFFER_HEIGHT) {
                int displayY = py - Board.BUFFER_HEIGHT;
                ge.drawImage(assetManager.tilePalette[pieceColorIndex],
                        boardOffsetX + px * TILE_SIZE + 1, // Adjusted X
                        displayY * TILE_SIZE + 1, // Adjusted Y
                        TILE_SIZE - 2, TILE_SIZE - 2);
            }
        }
    }

    private void drawGhostPiece(int playerIndex, int boardOffsetX) {
        if (currentPieces == null || currentPieces[playerIndex] == null || boards == null
                || boards[playerIndex] == null)
            return;

        int[][] ghostBlocks = currentPieces[playerIndex].getGhostCoordinates();
        Color base = tileColors[currentPieces[playerIndex].getColor()];
        Color translucent = new Color(base.getRed(), base.getGreen(), base.getBlue(), 88);
        ge.changeColor(translucent);

        for (int[] block : ghostBlocks) {
            int px = block[0];
            int py = block[1];
            if (py >= Board.BUFFER_HEIGHT) {
                int displayY = py - Board.BUFFER_HEIGHT;
                ge.drawSolidRectangle(boardOffsetX + px * TILE_SIZE + 1, // Adjusted X
                        displayY * TILE_SIZE + 1, // Adjusted Y
                        TILE_SIZE - 2, TILE_SIZE - 2);
            }
        }
    }

    private void drawScoreAndLevel(int playerIndex, int x, int yLabel) {
        if (scoreManagers == null || scoreManagers[playerIndex] == null)
            return;

        // yValue sits 15px below yLabel (to draw the numeric score)
        int yValue = yLabel + 15;

        // 1) Draw the semi‐transparent box (exactly as before)
        int padX = 10, padY = 5;
        int lineHeight = 18;
        int boxW = 4 + padX * 8; // same 4‐tile width + horizontal padding
        int boxH = lineHeight * 2 + padY * 14; // two lines of 18px text + vertical padding

        int boxX = x - padX; // extend padX left of x
        int boxY = yLabel - 10 - lineHeight - padY; // top of box sits above the first line

        ge.changeColor(new Color(50, 50, 50, 180));
        ge.drawSolidRectangle(boxX, boxY, boxW, boxH);
        ge.changeColor(ge.white);
        ge.drawRectangle(boxX, boxY, boxW, boxH, 4);

        // 2) Draw “SCORE:” label at (x, yLabel−10)
        ge.changeColor(ge.white);
        ge.drawBoldText(x, yLabel - 10, "SCORE:", 18);

        // 3) Center the numeric score inside the box
        String scoreValue = ge.toString(scoreManagers[playerIndex].getScore());
        int scoreW = ge.getFontMetrics().stringWidth(scoreValue);
        int scoreX = boxX + (boxW - scoreW) / 2;
        ge.drawText(scoreX, yValue, scoreValue, 18);

        // 4) Move down for “LEVEL:”
        yLabel += 40;
        yValue += 40;

        ge.drawBoldText(x, yLabel, "LEVEL:", 18);

        String levelValue = ge.toString(scoreManagers[playerIndex].getLevel());
        int levelW = ge.getFontMetrics().stringWidth(levelValue);
        int levelX = boxX + (boxW - levelW) / 2;
        ge.drawText(levelX, yValue + 10, levelValue, 18);
    }

    private void drawCountdown(int playerIndex, int playerAreaOffsetX, int boardVisualWidth) {
        if (gameStates == null || gameStates[playerIndex] == null)
            return;
        long remaining = gameStates[playerIndex].getCountdownRemaining();
        String text = remaining > 0 ? "" + remaining : "Go!";
        int textWidth = ge.getFontMetrics().stringWidth(text); // Requires GameEngine to have font metrics access
        // For simplicity, let's estimate centering for now
        int textX = playerAreaOffsetX + (boardVisualWidth / 2) - (textWidth / 2) - TILE_SIZE; // Approx center of board
                                                                                              // area

        ge.changeColor(ge.white);
        ge.drawBoldText(textX, 200, text, 50);
    }

    // Renamed to drawPlayerGameOverScreen to distinguish from a potential overall
    // one
    private void drawPlayerGameOverScreen(int playerIndex, int playerAreaOffsetX, int boardVisualWidth) {
        if (scoreManagers == null || scoreManagers[playerIndex] == null)
            return;
        String gameOverText = "Game Over!";
        String scoreText = "Final Score: " + scoreManagers[playerIndex].getScore();
        String levelText = "Level: " + scoreManagers[playerIndex].getLevel();

        // Approx centering
        int textX = playerAreaOffsetX + TILE_SIZE * 2; // Indent into player's board area

        ge.changeColor(ge.white);
        ge.drawBoldText(textX, 130, gameOverText, 30);
        ge.drawText(textX, 170, scoreText, 20);
        ge.drawText(textX, 190, levelText, 20);
        // Player-specific restart instruction might not be needed if global 'R' works
    }

    // Placeholder for a screen when ALL players are paused by global key
    private void drawGlobalPauseScreen() {
        // 1) Background (same as before) 
        ge.drawImage(assetManager.Background, 0, 0, ge.mWidth, ge.mHeight);
        ge.changeColor(new Color(0, 0, 0, 160));
        ge.drawSolidRectangle(0, 0, ge.mWidth, ge.mHeight);

        // 2) Create centered panel (same dimensions as ESC menu) 
        int panelW = Math.min(600, ge.mWidth - 80);
        int panelH = Math.min(400, ge.mHeight - 80);
        int panelX = (ge.mWidth - panelW) / 2;
        int panelY = (ge.mHeight - panelH) / 2;

        ge.changeColor(new Color(50, 50, 50, 200));
        ge.drawSolidRectangle(panelX, panelY, panelW, panelH);
        ge.changeColor(ge.white);
        ge.drawRectangle(panelX, panelY, panelW, panelH, 4);

        // 3) Draw title with same positioning as ESC menu 
        String title = "PAUSED";
        int titleFontSize = 48;
        ge.drawText(0, 0, "", titleFontSize);
        FontMetrics fm = ge.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = panelX + (panelW - titleW) / 2;
        int titleY = panelY + 75;
        ge.changeColor(ge.white);
        ge.drawBoldText(titleX, titleY, title, titleFontSize);

        // 4) Draw options with same box dimensions as ESC menu 
        String[] options = {
                "PRESS P TO RESUME",
                "PRESS R FOR MAIN MENU",
                "PRESS ESC FOR MAIN MENU"
        };

        int optionFontSize = 20;
        ge.drawText(0, 0, "", optionFontSize);
        fm = ge.getFontMetrics();
        int textH = fm.getAscent();
        int padX = 20;
        int padY = 12;
        int spacing = 20;

        // Calculate box dimensions exactly like ESC menu
        int maxTextW = 0;
        for (String opt : options) {
            maxTextW = Math.max(maxTextW, fm.stringWidth(opt));
        }
        int boxW = maxTextW + padX * 2;
        int boxH = textH + padY * 2;

        // Use same first baseline position as ESC menu
        int firstBaselineY = titleY + 85;

        for (int i = 0; i < options.length; i++) {
            String optText = options[i];
            int textW = fm.stringWidth(optText);
            int boxX = panelX + (panelW - boxW) / 2;
            int baselineY = firstBaselineY + i * (boxH + spacing);
            int boxY = baselineY - textH - padY;

            ge.changeColor(new Color(80, 80, 80, 200));
            ge.drawSolidRectangle(boxX, boxY, boxW, boxH);
            ge.changeColor(ge.white);
            ge.drawRectangle(boxX, boxY, boxW, boxH, 4);

            int textX = boxX + (boxW - textW) / 2;
            int textY = boxY + (boxH + textH) / 2 - 2;
            ge.changeColor(ge.white);
            ge.drawBoldText(textX, textY, optText, optionFontSize);
        }
    }

    private void drawEscPauseMenu() {
        // 1) Full‐window background + dimming overlay 
        ge.drawImage(assetManager.Background, 0, 0, ge.mWidth, ge.mHeight);
        ge.changeColor(new Color(0, 0, 0, 160)); // semi‐transparent black
        ge.drawSolidRectangle(0, 0, ge.mWidth, ge.mHeight);

        // 2) Enlarge the pause‐panel by +100px in both dimensions 
        // (Up from 500×300 → now 600×400, but still capped if window is smaller.)
        int panelW = Math.min(600, ge.mWidth - 80); // allow a bit more breathing room
        int panelH = Math.min(400, ge.mHeight - 80);
        int panelX = (ge.mWidth - panelW) / 2;
        int panelY = (ge.mHeight - panelH) / 2;

        ge.changeColor(new Color(50, 50, 50, 200)); // dark gray @ alpha 200
        ge.drawSolidRectangle(panelX, panelY, panelW, panelH);
        ge.changeColor(ge.white);
        ge.drawRectangle(panelX, panelY, panelW, panelH, 4); // 4px white border

        // 3) Draw “PAUSED” title centered inside that larger panel 
        String title = "PAUSED";
        int titleFontSize = 48;
        ge.drawText(0, 0, "", titleFontSize); // force FontMetrics to that size
        FontMetrics fm = ge.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = panelX + (panelW - titleW) / 2;
        int titleY = panelY + 75; // push it down a bit more
        ge.changeColor(ge.white);
        ge.drawBoldText(titleX, titleY, title, titleFontSize);

        // 4) Draw each option (“Resume”, “Help”, “Quit”), centered inside the panel
        // 
        String[] options = globalGameState.getPauseMenuOptions(); // still ["Resume","Help","Quit"]
        int selected = globalGameState.pauseMenuSelection;

        // Instead of modifying options[], copy each label to uppercase for drawing:
        String[] display = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            display[i] = options[i].toUpperCase();
        }

        int optionFontSize = 28;
        ge.drawText(0, 0, "", optionFontSize); // force FontMetrics to that size
        fm = ge.getFontMetrics();
        int textH = fm.getAscent();
        int padX = 20; // horizontal padding
        int padY = 12; // vertical padding
        int spacing = 20;// gap between boxes

        // Compute maximum text width among display[] for consistent box widths
        int maxTextW = 0;
        for (String opt : display) {
            maxTextW = Math.max(maxTextW, fm.stringWidth(opt));
        }
        int boxW = maxTextW + padX * 2;
        int boxH = textH + padY * 2;

        // First option’s baseline sits 30px below the title’s baseline:
        int firstBaselineY = titleY + 85;

        for (int i = 0; i < display.length; i++) {
            String optText = display[i];
            int textW = fm.stringWidth(optText);

            // Center the box X inside panel:
            int boxX = panelX + (panelW - boxW) / 2;
            // To draw the rectangle’s top, subtract textH + padY from its baseline
            int baselineY = firstBaselineY + i * (boxH + spacing);
            int boxY = baselineY - textH - padY;

            // 4a) Background (highlight if selected)
            if (i == selected) {
                ge.changeColor(new Color(180, 200, 0, 200)); // yellow–green @ alpha 200
            } else {
                ge.changeColor(new Color(80, 80, 80, 200)); // darker gray
            }
            ge.drawSolidRectangle(boxX, boxY, boxW, boxH);

            // 4b) White border around the box
            ge.changeColor(ge.white);
            ge.drawRectangle(boxX, boxY, boxW, boxH, 4);

            // 4c) Center the uppercase text inside the box
            int textX = boxX + (boxW - textW) / 2;
            int textY = boxY + (boxH + textH) / 2 - 2; // small nudge upward
            ge.changeColor(ge.white);
            ge.drawBoldText(textX, textY, optText, optionFontSize);
        }
    }

    // Placeholder for a screen when the game is truly over for everyone
    private void drawOverallGameOverScreen() {
        // 1) Full-window background + dimming overlay 
        ge.drawImage(assetManager.Background, 0, 0, ge.mWidth, ge.mHeight);
        ge.changeColor(new Color(0, 0, 0, 160)); // semi-transparent black
        ge.drawSolidRectangle(0, 0, ge.mWidth, ge.mHeight);

        // 2) Create centered panel 
        int panelW = Math.min(600, ge.mWidth - 80);
        int panelH = Math.min(400, ge.mHeight - 80);
        int panelX = (ge.mWidth - panelW) / 2;
        int panelY = (ge.mHeight - panelH) / 2;

        ge.changeColor(new Color(50, 50, 50, 200));
        ge.drawSolidRectangle(panelX, panelY, panelW, panelH);
        ge.changeColor(ge.white);
        ge.drawRectangle(panelX, panelY, panelW, panelH, 4);

        // 3) Draw "GAME OVER" title 
        String title = "GAME OVER";
        int titleFontSize = 48;
        ge.drawText(0, 0, "", titleFontSize);
        FontMetrics fm = ge.getFontMetrics();
        int titleW = fm.stringWidth(title);
        int titleX = panelX + (panelW - titleW) / 2;
        int titleY = panelY + 75;
        ge.changeColor(ge.white);
        ge.drawBoldText(titleX, titleY, title, titleFontSize);

        // 4) Draw final scores or winner 
        List<String> info = new ArrayList<>();

        if (game.getActivePlayers() == 1) {
            info.add("FINAL SCORE: " + scoreManagers[0].getScore());
            info.add("LEVEL: " + scoreManagers[0].getLevel());
        } else {
            // Find winner
            int highestScore = -1;
            int winnerIndex = -1;
            for (int i = 0; i < game.getActivePlayers(); i++) {
                int score = scoreManagers[i].getScore();
                if (score > highestScore) {
                    highestScore = score;
                    winnerIndex = i;
                }
            }
            info.add("WINNER: PLAYER " + (winnerIndex + 1));
            info.add("SCORE: " + highestScore);
        }
        info.add("PRESS R TO RETURN TO MENU");

        // Adjust font sizes and box dimensions based on player count
        int optionFontSize = game.getActivePlayers() == 1 ? 20 : 28; // Smaller font for 1P
        int padX = game.getActivePlayers() == 1 ? 15 : 20; // Smaller padding for 1P
        int padY = game.getActivePlayers() == 1 ? 8 : 12; // Smaller padding for 1P
        int spacing = game.getActivePlayers() == 1 ? 15 : 20; // Smaller spacing for 1P

        // Draw options in boxes
        ge.drawText(0, 0, "", optionFontSize);
        fm = ge.getFontMetrics();
        int textH = fm.getAscent();

        // Calculate box dimensions
        int maxTextW = 0;
        for (String text : info) {
            maxTextW = Math.max(maxTextW, fm.stringWidth(text));
        }
        int boxW = maxTextW + padX * 2;
        int boxH = textH + padY * 2;

        // Draw each info box with adjusted positioning
        int firstBaselineY = titleY + (game.getActivePlayers() == 1 ? 85 : 85); // Closer to title in 1P
        for (int i = 0; i < info.size(); i++) {
            String text = info.get(i);
            int textW = fm.stringWidth(text);
            int boxX = panelX + (panelW - boxW) / 2;
            int baselineY = firstBaselineY + i * (boxH + spacing);
            int boxY = baselineY - textH - padY;

            // Use green highlight for "PRESS R TO RETURN TO MENU"
            if (i == info.size() - 1) { // Last option
                ge.changeColor(new Color(180, 200, 0, 180)); // Same green as selected menu items
            } else {
                ge.changeColor(new Color(80, 80, 80, 200));
            }
            ge.drawSolidRectangle(boxX, boxY, boxW, boxH);
            ge.changeColor(ge.white);
            ge.drawRectangle(boxX, boxY, boxW, boxH, 4);

            int textX = boxX + (boxW - textW) / 2;
            int textY = boxY + (boxH + textH) / 2 - 2;
            ge.changeColor(ge.white);
            ge.drawBoldText(textX, textY, text, optionFontSize);
        }
    }

    // For individual player pause (if implemented and different from global)
    private void drawPlayerPauseMenu(int playerIndex, int playerAreaOffsetX, int boardVisualWidth) {
        if (gameStates == null || gameStates[playerIndex] == null)
            return;
        String text = "Player " + (playerIndex + 1) + " Paused";
        int textWidth = ge.getFontMetrics().stringWidth(text);
        int textX = playerAreaOffsetX + (boardVisualWidth / 2) - (textWidth / 2) - TILE_SIZE;

        ge.changeColor(ge.white);
        ge.drawBoldText(textX, 150, text, 24);
        // Individual pause options could be drawn here if they differ from global
        // For now, TetrisGame handles global pause menu logic. This is just a visual
        // indicator.
    }

    // Help screen: Assuming global for now, as in original.
    // If help becomes player-specific, this would need playerIndex and offsetX.
    private void drawHelpScreen() {
        // Draw background image and overlay
        ge.drawImage(assetManager.Background, 0, 0, ge.mWidth, ge.mHeight);
        ge.changeColor(new Color(0, 0, 0, 160)); // semi-transparent black
        ge.drawSolidRectangle(0, 0, ge.mWidth, ge.mHeight);

        // Create centered panel like other menus
        int panelW = Math.min(750, ge.mWidth - 80);
        int panelH = Math.min(400, ge.mHeight - 60);
        int panelX = (ge.mWidth - panelW) / 2;
        int panelY = 30; // Changed from panelH - 370 to fixed 30px from top

        ge.changeColor(new Color(50, 50, 50, 200));
        ge.drawSolidRectangle(panelX, panelY, panelW, panelH);
        ge.changeColor(ge.white);
        ge.drawRectangle(panelX, panelY, panelW, panelH, 4);

        // Rest of the help screen content, but adjusted to draw inside panel
        boolean useSmallLayout = ge.mWidth < 500;
        int headerFontSize = useSmallLayout ? 18 : 20;
        int detailFontSize = useSmallLayout ? 15 : 18;
        int mainXOffset = panelX + 30; // Adjust to be relative to panel

        ge.changeColor(ge.white);
        ge.drawBoldText(310, 75, "HOW TO PLAY", 30);

        // Player 1 controls section with fixed coordinates:
        ge.drawBoldText(mainXOffset, 110, "Player 1 (Left Side):", headerFontSize);
        ge.drawText(mainXOffset, 135, "Arrow Keys: Move Left/Right/Down", detailFontSize);
        ge.drawText(mainXOffset, 160, "Up Arrow: Rotate Clockwise", detailFontSize);
        ge.drawText(mainXOffset, 185, "Z: Rotate Counter-Clockwise", detailFontSize);
        ge.drawText(mainXOffset, 210, "C: Hold Piece", detailFontSize);
        ge.drawText(mainXOffset, 235, "Space: Hard Drop", detailFontSize);

        // Player 2 controls section (now always shown)
        ge.drawBoldText(460, 110, "Player 2 (Right Side):", headerFontSize);
        ge.drawText(480, 135, "A, D, S: Move Left/Right/Down", detailFontSize);
        ge.drawText(480, 160, "W: Rotate Clockwise", detailFontSize);
        ge.drawText(480, 185, "Q: Rotate Counter-Clockwise", detailFontSize);
        ge.drawText(480, 210, "E: Hold Piece", detailFontSize);
        ge.drawText(480, 235, "Shift (Left/Right): Hard Drop", detailFontSize);

        ge.drawBoldText(mainXOffset, 285, "Global Controls:", headerFontSize);
        ge.drawText(mainXOffset, 310, "Global: P - Pause Game", detailFontSize);
        ge.drawText(mainXOffset, 335, "Global: R - To Menu (Paused/Game Over)", detailFontSize);
        ge.drawText(mainXOffset, 360, "Global: Esc - Pause Menu / Resume", detailFontSize);

        ge.drawBoldText(mainXOffset, 395, "Press Esc to close Help", headerFontSize); // Slightly larger for this
        // important instruction

        ge.drawBoldText(460, 285, "Music Control:", detailFontSize);
        ge.drawText(480, 310, "1 = Track 1 (default)", detailFontSize);
        ge.drawText(480, 335, "2 = Track 2", detailFontSize);
        ge.drawText(480, 360, "3 = Track 3", detailFontSize);
        ge.drawText(480, 385, "0 = Stop music", detailFontSize);

    }

    private void drawNextPieces(int playerIndex, int x, int yLabel) {
        if (currentPieces == null || currentPieces[playerIndex] == null)
            return;

        // 1) Determine vertical layout:
        // - yLabel is where “NEXT:” text baseline will sit (you passed this in).
        // - After that, previews stack downward with PREVIEW_SPACING_Y each.
        List<Integer> nextPieceTypes = currentPieces[playerIndex].getNextPieces();

        // 2) Calculate the box dimensions
        FontMetrics fm = ge.getFontMetrics();
        int labelH = fm.getAscent(); // height of “NEXT:” text

        int padX = 10, padY = 5;
        // width = 4 preview tiles + horizontal padding
        int boxW = PREVIEW_PIECE_SIZE * 4 + padX * 2;
        // compute total preview height (each preview is PREVIEW_PIECE_SIZE*2.5 tall):
        int previewsH = (int) (PREVIEW_PIECE_SIZE * 2.5) * nextPieceTypes.size()
                + PREVIEW_SPACING_Y * (nextPieceTypes.size() - 1);
        // total height = label + previews + vertical padding (top/bottom + between
        // label & first preview)
        int boxH = labelH
                + previewsH
                + padY * 2 // top and bottom padding
                + padY - 55; // extra pad between label and first preview

        // 3) Compute boxX/boxY so that the box surrounds label+previews
        int boxX = x - padX;
        int boxY = yLabel - 10 - labelH - padY;

        // 4) Draw semi-transparent background rectangle
        ge.changeColor(new Color(50, 50, 50, 180));
        ge.drawSolidRectangle(boxX, boxY, boxW, boxH);

        // 5) Draw white border around it
        ge.changeColor(ge.white);
        ge.drawRectangle(boxX, boxY, boxW, boxH, 4);

        // 6) Draw the “NEXT:” label
        ge.changeColor(ge.white);
        ge.drawBoldText(x, yLabel - 5, "NEXT:", 18);

        // 7) Draw each preview box & tile graphic:
        for (int i = 0; i < nextPieceTypes.size(); i++) {
            int previewY = yLabel + padY + (i * PREVIEW_SPACING_Y);

            // background for the tile area
            ge.changeColor(40, 40, 40);
            ge.drawSolidRectangle(x, previewY,
                    PREVIEW_PIECE_SIZE * 4,
                    (int) (PREVIEW_PIECE_SIZE * 2.5));

            // draw the tetromino preview
            drawPreviewPiece(nextPieceTypes.get(i), x, previewY);
        }
    }

    private void drawHoldPiece(int playerIndex, int x, int yLabel) {
        if (game == null)
            return;

        // The “HOLD:” text is drawn at (x, yLabel–5). The preview (tile grid) sits just
        // below:
        int yPreview = yLabel + 5;

        // 1) Draw a background box behind “HOLD” + preview
        FontMetrics fm = ge.getFontMetrics();
        int textH = fm.getAscent(); // height of “HOLD:” text
        int padX = 10, padY = 5;
        int boxW = PREVIEW_PIECE_SIZE * 4 + padX * 2; // four previews across + padding
        int boxH = textH + (int) (PREVIEW_PIECE_SIZE * 2.5) + padY * 6;

        int boxX = x - padX;
        int boxY = yLabel - 10 - textH - padY; // top of box sits above “HOLD:” text

        ge.changeColor(new Color(50, 50, 50, 180));
        ge.drawSolidRectangle(boxX, boxY, boxW, boxH);
        ge.changeColor(ge.white);
        ge.drawRectangle(boxX, boxY, boxW, boxH, 4);

        // 2) Draw the “HOLD:” label
        ge.changeColor(ge.white);
        ge.drawBoldText(x, yLabel - 5, "HOLD:", 18);

        // 3) Draw the preview tiles underneath
        Integer heldType = game.getHeldPieceType(playerIndex);
        if (heldType != null) {
            ge.changeColor(40, 40, 40);
            ge.drawSolidRectangle(x, yPreview,
                    PREVIEW_PIECE_SIZE * 4,
                    PREVIEW_PIECE_SIZE * 2.5);
            drawPreviewPiece(heldType, x, yPreview);
        }
    }

    private void drawPreviewPiece(int pieceType, int x, int y) {
        int[][] shape = Piece.SHAPES[pieceType];
        int pieceColorIndex = pieceType + 1; // Same indexing as main pieces

        // Use original offset calculations
        int offsetX = 25;
        int offsetY = 5;

        if (pieceType == 0) { // I piece
            offsetX = 15;
            offsetY = 12;
        } else if (pieceType == 3) { // O piece
            offsetX = 15;
        }

        for (int[] block : shape) {
            int blockX = x + offsetX + (block[0] * PREVIEW_PIECE_SIZE);
            int blockY = y + offsetY + (block[1] * PREVIEW_PIECE_SIZE);
            // Use tile assets instead of solid rectangles
            ge.drawImage(assetManager.tilePalette[pieceColorIndex],
                    blockX,
                    blockY,
                    PREVIEW_PIECE_SIZE - 1,
                    PREVIEW_PIECE_SIZE - 1);
        }
    }

    private void drawGameModeMenu() {
        ge.changeColor(ge.white);

        // Logo drawing
        int targetWidth = ge.mWidth / 3;
        int originalW = assetManager.Logo.getWidth(null);
        int originalH = assetManager.Logo.getHeight(null);
        int targetHeight = (int) ((double) originalH / originalW * targetWidth);
        int logoX = (ge.mWidth - targetWidth) / 2;
        int logoY = 20;
        ge.drawImage(assetManager.Logo, logoX, logoY, targetWidth, targetHeight);

        // right after you draw the logo, before drawing the options:
        // anchor the first option 15px below the logo
        int firstOptionY = logoY + targetHeight + 20;
        int startY = firstOptionY;
        int optionFontSize = 24;
        // force the engine to use that font for metrics
        ge.drawText(0, 0, "", optionFontSize);

        String[] options = globalGameState.getGameModeMenuOptions();
        int selected = globalGameState.gameModeMenuSelection;

        // padding & spacing
        int padX = 20;
        int padY = 10;
        int spacing = 15;

        // grab FontMetrics once (now at size 24)
        FontMetrics fm = ge.getFontMetrics();
        int textH = fm.getAscent();
        int boxH = textH + padY * 2;

        for (int i = 0; i < options.length; i++) {
            String opt = options[i].toUpperCase(); // Convert to uppercase
            int textW = fm.stringWidth(opt);
            int boxW = textW + padX * 2;
            int boxX = (ge.mWidth - boxW) / 2;
            int boxY = startY + i * (boxH + spacing);

            // background: dark grey vs green-yellow
            if (i == selected) {
                ge.changeColor(new Color(180, 200, 0, 180)); // yellow-green, alpha 180
            } else {
                ge.changeColor(new Color(50, 50, 50, 180)); // grey, alpha 180
            }
            ge.drawSolidRectangle(boxX, boxY, boxW, boxH);

            // border: bright yellow-green vs white
            if (i == selected) {
                ge.changeColor(new Color(240, 255, 0)); // solid yellow-green
            } else {
                ge.changeColor(ge.white);
            }
            ge.drawRectangle(boxX, boxY, boxW, boxH, 4);

            // centered text
            int textX = boxX + (boxW - textW) / 2;
            int textY = boxY + (boxH + textH) / 2;
            ge.changeColor(ge.white);
            ge.drawBoldText(textX, textY, opt, optionFontSize);
        }

    }

    // Draw a quick white bar across each cleared row, fading out
    // Draw a glowing, expanding flash on each cleared row
    private void drawRowFlashes(int boardOffsetX) {
        List<?> rawFlashes = game.getRowFlashes();
        for (Object obj : rawFlashes) {
            TetrisGame.RowFlash f = (TetrisGame.RowFlash) obj;

            float alpha = f.alpha();
            double scale = f.heightScale();

            // Base Y (board coords → pixels)
            double py = f.rowY * TILE_SIZE;

            // centered vertically around the original tile Y:
            double halfHeight = (TILE_SIZE * (scale - 1.0)) / 2.0;
            double drawY = py - halfHeight;

            // Total draw‐height = TILE_SIZE * scale
            double drawH = TILE_SIZE * scale;
            double drawW = Board.WIDTH * TILE_SIZE;
            double drawX = boardOffsetX;

            // 1) Draw the inner white core (brightest)
            // - full alpha peak, but we modulate by f.alpha()
            Color core = new Color(255, 255, 255, (int) (255 * alpha));
            ge.changeColor(core);
            ge.drawSolidRectangle(drawX, drawY, drawW, drawH);

            // 2) Draw a slightly larger, pale‐blue “glow” behind it
            // - use the same alpha but lower opacity (e.g. 50% of core)
            int glowAlpha = (int) (128 * alpha);
            Color glow = new Color(180, 220, 255, glowAlpha);
            ge.changeColor(glow);

            // expand 2px on all sides for the glow
            ge.drawSolidRectangle(drawX - 2, drawY - 2, drawW + 4, drawH + 4);
        }
    }

    // Draw the previously spawned Particle squares (white, flying sideways)
    private void drawParticles(int boardOffsetX) {
        List<?> rawList = game.getParticles();
        for (Object obj : rawList) {
            TetrisGame.Particle p = (TetrisGame.Particle) obj;

            float alpha = p.alpha();
            Color c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(),
                    (int) (alpha * 255f));
            ge.changeColor(c);

            double half = p.size / 2.0;
            ge.drawSolidRectangle(p.x - half, p.y - half, p.size, p.size);
        }
    }

    private void drawPopups(int playerIndex, int boardOffsetX) {
        if (boards == null || boards[playerIndex] == null) return;
        
        // Draw score popups
        for (ScorePopup popup : boards[playerIndex].getScorePopups()) {
            Color textColor = new Color(1.0f, 1.0f, 1.0f, popup.getAlpha());
            ge.changeColor(textColor);
            
            int centerX = boardOffsetX + (Board.WIDTH * TILE_SIZE) / 2;
            int fontSize = 18;
            ge.drawBoldText(centerX - 30, popup.getY(), popup.getScoreType(), fontSize);
            ge.drawBoldText(centerX - 20, popup.getY() + 20, String.valueOf(popup.getScoreValue()), fontSize);
        }
        
        // Draw level up popups
        for (LevelUpPopup popup : boards[playerIndex].getLevelUpPopups()) {
            Color textColor = new Color(1.0f, 1.0f, 0.2f, popup.getAlpha());
            ge.changeColor(textColor);
            
            int centerX = boardOffsetX + (Board.WIDTH * TILE_SIZE) / 2;
            int fontSize = (int)(24 * popup.getScale());
            String text = "LEVEL " + popup.getLevel() + "!";
            ge.drawBoldText(centerX - (fontSize * 2), popup.getY(), text, fontSize);
        }
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }
}