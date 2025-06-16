public class GameState {
    private boolean isGameOver;
    private boolean isPaused;
    private boolean showHelp;
    private boolean showCountdown;

    private String[] gameModeMenuOptions = { "1 Player", "2 Players", "Player vs AI", "Help", "Quit" };
    public int gameModeMenuSelection = 0; // 0 for 1P, 1 for 2P, 2 for Help, 3 for Quit

    private String[] pauseMenuOptions = { "Resume", "Help", "Quit" }; // Options for the ESC pause menu
    public int pauseMenuSelection = 0; // 0 for 1P, 1 for 2P, 2 for PvAI, 3 for Help, 4 for Quit

    // Game Mode and Main Menu
    private GameMode currentMode; // Current overall game mode

    private long countdownStartTime;
    private int countdownSeconds = 3;

    public GameState() {
        reset();
    }

    public void reset() {
        isGameOver = false;
        isPaused = false;
        showHelp = false;
        showCountdown = false;
        pauseMenuSelection = 0;
        currentMode = GameMode.MENU; // Start in menu mode
        gameModeMenuSelection = 0;
    }

    // Getters and Setters
    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }

    public boolean isShowCountdown() {
        return showCountdown;
    }

    public void setShowCountdown(boolean showCountdown) {
        this.showCountdown = showCountdown;
        if (showCountdown) {
            countdownStartTime = System.currentTimeMillis();
        }
    }

    public void updateCountdown() {
        if (showCountdown) {
            long elapsed = (long) ((System.currentTimeMillis() - countdownStartTime) / (1000 / 1.5));
            if (elapsed >= countdownSeconds) {
                showCountdown = false;
            }
        }
    }

    public long getCountdownRemaining() {
        if (!showCountdown)
            return 0;
        long elapsed = (long) ((System.currentTimeMillis() - countdownStartTime) / (1000 / 1.5));
        return Math.max(0, countdownSeconds - elapsed);
    }

    public String[] getPauseMenuOptions() {
        return pauseMenuOptions;
    }

    public void nextPauseOption() {
        pauseMenuSelection = (pauseMenuSelection + 1) % pauseMenuOptions.length;
    }

    public void previousPauseOption() {
        pauseMenuSelection = (pauseMenuSelection + pauseMenuOptions.length - 1) % pauseMenuOptions.length;
    }

    public String getSelectedPauseOption() {
        return pauseMenuOptions[pauseMenuSelection];
    }

    // Game Mode and Main Menu methods
    public GameMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(GameMode mode) {
        this.currentMode = mode;
    }

    public String[] getGameModeMenuOptions() {
        return gameModeMenuOptions;
    }

    public void nextGameModeOption() {
        gameModeMenuSelection = (gameModeMenuSelection + 1) % gameModeMenuOptions.length;
    }

    public void previousGameModeOption() {
        gameModeMenuSelection = (gameModeMenuSelection + gameModeMenuOptions.length - 1) % gameModeMenuOptions.length;
    }

    public String getSelectedGameModeOption() {
        return gameModeMenuOptions[gameModeMenuSelection];
    }
}
