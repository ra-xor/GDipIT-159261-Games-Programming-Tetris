import java.awt.event.KeyEvent;

public class InputHandler {
    // DAS/ARR (Delayed Auto Shift/Auto Repeat, horizontal movement)
    private double dasDelay = 0.15;
    private double arrInterval = 0.05;
    private double leftHeldTime = 0, rightHeldTime = 0;
    private boolean leftHeld = false, rightHeld = false;

    private boolean softDropping = false;

    private TetrisGame game; // Reference to the main game logic
    private int playerIndex; // To identify which player this handler is for

    // Key mappings - these could be made configurable later
    private int keyLeft;
    private int keyRight;
    private int keyDown;
    private int keyHardDrop;
    private int keyRotateClockwise;
    private int keyRotateCounterClockwise;
    private int keyHold;

    public InputHandler(TetrisGame game, int playerIndex) {
        this.game = game;
        this.playerIndex = playerIndex;
        setPlayerKeys();
    }

    private void setPlayerKeys() {
        if (playerIndex == 0) { // Player 1
            keyLeft = KeyEvent.VK_LEFT;
            keyRight = KeyEvent.VK_RIGHT;
            keyDown = KeyEvent.VK_DOWN;
            keyHardDrop = KeyEvent.VK_SPACE;
            keyRotateClockwise = KeyEvent.VK_UP;
            keyRotateCounterClockwise = KeyEvent.VK_Z; // Or another key like CTRL
            keyHold = KeyEvent.VK_C;
        } else if (playerIndex == 1) { // Player 2
            keyLeft = KeyEvent.VK_A;
            keyRight = KeyEvent.VK_D;
            keyDown = KeyEvent.VK_S;
            keyHardDrop = KeyEvent.VK_SHIFT; // Using Shift for P2 hard drop
            keyRotateClockwise = KeyEvent.VK_W;
            keyRotateCounterClockwise = KeyEvent.VK_Q;
            keyHold = KeyEvent.VK_E;
        }
        // Add more players as needed with different key sets
    }

    public void update(double dt) {
        // Use player-specific game state
        if (game.isOverallGameOver() || game.isGloballyPaused() || game.getPlayerGameState(playerIndex) == null
                || game.getPlayerGameState(playerIndex).isGameOver()
                || game.getPlayerGameState(playerIndex).isShowCountdown()) {
            // If global game over/pause, or this player is game over/in countdown, do
            // nothing for this player
            // Added null check for getPlayerGameState(playerIndex) as an extra precaution
            return;
        }

        // DAS/ARR for L/R movement, now calls player-specific methods
        if (leftHeld) {
            leftHeldTime += dt;
            if (leftHeldTime >= dasDelay) {
                int repeats = (int) ((leftHeldTime - dasDelay) / arrInterval);
                for (int i = 0; i < repeats; i++)
                    game.moveActivePieceLeft(playerIndex);
                leftHeldTime = dasDelay + (leftHeldTime - dasDelay) % arrInterval;
            }
        }
        if (rightHeld) {
            rightHeldTime += dt;
            if (rightHeldTime >= dasDelay) {
                int repeats = (int) ((rightHeldTime - dasDelay) / arrInterval);
                for (int i = 0; i < repeats; i++)
                    game.moveActivePieceRight(playerIndex);
                rightHeldTime = dasDelay + (rightHeldTime - dasDelay) % arrInterval;
            }
        }
        if (softDropping && game.getCurrentPiece(playerIndex).moveDown()) {
            game.getRenderer().getAssetManager().playSound(game.getRenderer().getAssetManager().softDropSound);
        }
    }

    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Global controls (pause, restart) are handled in TetrisGame.java directly.
        // This handler only processes player-specific actions.

        // Player-specific game state checks
        if (game.isOverallGameOver() || game.isGloballyPaused() || game.getPlayerGameState(playerIndex) == null
                || game.getPlayerGameState(playerIndex).isGameOver() || game.getPlayerGameState(playerIndex).isPaused()
                || game.getPlayerGameState(playerIndex).isShowCountdown()) {
            // No actions if game is over, paused, or in countdown for this player
            // Added null check
            return;
        }

        // Active game key presses for this player
        if (keyCode == keyLeft) {
            leftHeld = true;
            leftHeldTime = 0;
            game.moveActivePieceLeft(playerIndex);
        } else if (keyCode == keyRight) {
            rightHeld = true;
            rightHeldTime = 0;
            game.moveActivePieceRight(playerIndex);
        } else if (keyCode == keyDown) {
            softDropping = true;
            // Make sure piece exists and is not landed before trying to move it or reset
            // timer
            if (game.getCurrentPiece(playerIndex) != null && !game.getCurrentPiece(playerIndex).isLanded()) {
                game.getCurrentPiece(playerIndex).moveDown(); // Move one step immediately
                game.resetFallTimer(playerIndex); // Reset fall timer to reflect soft drop
            }
        } else if (keyCode == keyHardDrop) {
            game.hardDropActivePiece(playerIndex);
        } else if (keyCode == keyRotateClockwise) {
            game.rotateActivePiece(playerIndex, true);
        } else if (keyCode == keyRotateCounterClockwise) {
            game.rotateActivePiece(playerIndex, false);
        } else if (keyCode == keyHold) {
            game.holdActivePiece(playerIndex);
        }
    }

    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == keyLeft) {
            leftHeld = false;
            leftHeldTime = 0; // Reset time on release
        } else if (keyCode == keyRight) {
            rightHeld = false;
            rightHeldTime = 0; // Reset time on release
        } else if (keyCode == keyDown) {
            softDropping = false;
        }
    }

    public boolean isSoftDropping() {
        return softDropping;
    }

    public void resetDAS() {
        leftHeld = false;
        rightHeld = false;
        leftHeldTime = 0;
        rightHeldTime = 0;
        softDropping = false;
    }
}
