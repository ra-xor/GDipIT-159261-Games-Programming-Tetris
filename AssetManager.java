import java.awt.Image;

public class AssetManager {
    private GameEngine engine; // to pass specific loadImage/loadAudio methods

    // assets paths
    public static final String IMGS_PATH = "tetris/assets/imgs/";
    public static final String WAV_PATH = "tetris/assets/wav/";

    private Image blue, cyan, green, orange, purple, red, yellow, black, grey;

    public Image Background;

    public Image Logo;

    // Tile palette - order matters!
    public Image[] tilePalette;
    // public Image[] tileGhost; to be used in future for ghost pieces

    // Sound effects
    public GameEngine.AudioClip rotateSound;
    public GameEngine.AudioClip moveSound;
    public GameEngine.AudioClip lockSound;
    public GameEngine.AudioClip gameOverSound;
    public GameEngine.AudioClip singleLineSound;
    public GameEngine.AudioClip doubleLineSound;
    public GameEngine.AudioClip tripleLineSound;
    public GameEngine.AudioClip tetrisSound;
    public GameEngine.AudioClip softDropSound;
    public GameEngine.AudioClip hardDropSound;
    public GameEngine.AudioClip holdSound;
    public GameEngine.AudioClip blockedMoveSound;
    public GameEngine.AudioClip blockedRotateSound;
    public GameEngine.AudioClip levelUpSound;


    // Background Music
    private GameEngine.AudioClip[] backgroundTracks;
    private GameEngine.AudioClip currentMusic;
    private boolean isMusicPlaying;
    private int currentTrackIndex;
    private static final int TRACK_COUNT = 3;

    // Background Music durations in seconds
    private static final double[] TRACK_DURATIONS = {
        292.0,  // bgm_1 (4:52)
        83.0,   // bgm_2 (1:23)
        290.0   // bgm_3 (4:50)
    };
    private double musicTimer;

    public AssetManager(GameEngine ge) {
        this.engine = ge;
        this.backgroundTracks = new GameEngine.AudioClip[TRACK_COUNT];
        this.isMusicPlaying = false;
        this.currentTrackIndex = 0;
    }

    public void loadAll() {
        // Tetramino assets
        blue = engine.loadImage(IMGS_PATH + "blue.png");
        cyan = engine.loadImage(IMGS_PATH + "cyan.png");
        green = engine.loadImage(IMGS_PATH + "green.png");
        orange = engine.loadImage(IMGS_PATH + "orange.png");
        purple = engine.loadImage(IMGS_PATH + "purple.png");
        red = engine.loadImage(IMGS_PATH + "red.png");
        yellow = engine.loadImage(IMGS_PATH + "yellow.png");
        black = engine.loadImage(IMGS_PATH + "black.png");
        grey = engine.loadImage(IMGS_PATH + "grey.png");

        // Background and logo
        Background = engine.loadImage(IMGS_PATH + "Background.png");
        Logo = engine.loadImage(IMGS_PATH + "Logo.png");

        // Tile palette - order matters!
        tilePalette = new Image[] {
                black, // 0 = empty cell
                cyan, // 1 = I piece
                blue, // 2 = J piece
                orange, // 3 = L piece
                yellow, // 4 = O piece
                green, // 5 = S piece
                purple, // 6 = T piece
                red, // 7 = Z piece
                grey // 8 = Garbage tile
        };

        // Sounds
        // define sound assets for tetromino game, like rotate, soft drop, hard drop, etc.
        // Movement and Rotation
        rotateSound = engine.loadAudio(WAV_PATH + "rotate.wav");
        moveSound = engine.loadAudio(WAV_PATH + "move.wav");
        lockSound = engine.loadAudio(WAV_PATH + "lock.wav");

        // Line Clears
        singleLineSound = engine.loadAudio(WAV_PATH + "single.wav");
        doubleLineSound = engine.loadAudio(WAV_PATH + "double.wav");
        tripleLineSound = engine.loadAudio(WAV_PATH + "triple.wav");
        tetrisSound = engine.loadAudio(WAV_PATH + "tetris.wav");

        // Drops and Hold
        softDropSound = engine.loadAudio(WAV_PATH + "soft_drop.wav");
        hardDropSound = engine.loadAudio(WAV_PATH + "hard_drop.wav");
        holdSound = engine.loadAudio(WAV_PATH + "hold.wav");

        // Failure and Game States
        blockedMoveSound = engine.loadAudio(WAV_PATH + "blocked_move.wav");
        blockedRotateSound = engine.loadAudio(WAV_PATH + "blocked_rotate.wav");
        levelUpSound = engine.loadAudio(WAV_PATH + "level_up.wav");
        gameOverSound = engine.loadAudio(WAV_PATH + "game_over.wav");

        // Load background music tracks
        backgroundTracks[0] = engine.loadAudio(WAV_PATH + "bgm_1.wav");
        backgroundTracks[1] = engine.loadAudio(WAV_PATH + "bgm_2.wav");
        backgroundTracks[2] = engine.loadAudio(WAV_PATH + "bgm_3.wav");
    }

    // Helper method to play sounds with consistent volume
    public void playSound(GameEngine.AudioClip sound) {
        if (sound != null) {
            engine.playAudio(sound);
        }
    }

    public void handleMusicInput(char key) {
        stopMusic(); // Stop current music if playing

        switch(key) {
            case '1':
                currentTrackIndex = 0;
                startMusic();
                break;
            case '2':
                currentTrackIndex = 1;
                startMusic();
                break;
            case '3':
                currentTrackIndex = 2;
                startMusic();
                break;
            case '0':
                currentTrackIndex = -1; // No music
                break;
        }
    }

    public void startMusic() {
        if (currentTrackIndex >= 0 && currentTrackIndex < backgroundTracks.length) {
            currentMusic = backgroundTracks[currentTrackIndex];
            if (currentMusic != null) {
                engine.startAudioLoop(currentMusic);  // Changed from startAudioLoop
                musicTimer = 0.0;  // Reset timer
                isMusicPlaying = true;
            }
        }
    }

    // Add this method to update music
    public void updateMusic(double dt) {
        if (isMusicPlaying && currentTrackIndex >= 0) {
            musicTimer += dt;
            if (musicTimer >= TRACK_DURATIONS[currentTrackIndex]) {
                // Restart the track
                engine.stopAudioLoop(currentMusic);
                engine.startAudioLoop(currentMusic);
                musicTimer = 0.0;
            }
        }
    }

    public void stopMusic() {
        if (currentMusic != null && isMusicPlaying) {
            engine.stopAudioLoop(currentMusic);  // Changed from stopAudioLoop
            isMusicPlaying = false;
            musicTimer = 0.0;
        }
    }
}