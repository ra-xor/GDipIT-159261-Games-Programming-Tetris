public class GameEngine {
    protected int mWidth, mHeight;
    protected java.awt.Color black = java.awt.Color.BLACK;
    protected java.awt.Color cyan = java.awt.Color.CYAN;
    protected java.awt.Color blue = java.awt.Color.BLUE;
    protected java.awt.Color orange = java.awt.Color.ORANGE;
    protected java.awt.Color yellow = java.awt.Color.YELLOW;
    protected java.awt.Color green = java.awt.Color.GREEN;
    protected java.awt.Color pink = java.awt.Color.PINK;
    protected java.awt.Color red = java.awt.Color.RED;
    protected java.awt.Color white = java.awt.Color.WHITE;

    public static <T extends GameEngine> void createGame(T game, int fps) {}

    public void setWindowSize(int w, int h) {
        this.mWidth = w;
        this.mHeight = h;
    }

    public void clearBoard() {}
    public void changeBackgroundColor(java.awt.Color c) {}
    public void clearBackground(int w, int h) {}
    public void drawSolidRectangle(int x, int y, int w, int h) {}
    public void drawBoldText(int x, int y, String text, int size) {}
    public void drawText(int x, int y, String text, int size) {}
    public void changeColor(int r, int g, int b) {}
    public void changeColor(java.awt.Color c) {}
    public int rand(int max) { return 0; }
    public void paintComponent() {}
}
