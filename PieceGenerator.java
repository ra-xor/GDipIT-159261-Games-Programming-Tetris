import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PieceGenerator {
    private List<Integer> currentBag;
    private List<Integer> nextBag;
    private final int BAG_SIZE = 7;
    private final int PREVIEW_SIZE = 3;
    private Random random;

    public PieceGenerator() {
        this.random = new Random();
        currentBag = new ArrayList<>();
        nextBag = new ArrayList<>();
        fillNewBag(currentBag);
        fillNewBag(nextBag);
    }

    private void fillNewBag(List<Integer> bag) {
        bag.clear();
        for (int i = 0; i < BAG_SIZE; i++) {
            bag.add(i);
        }
        Collections.shuffle(bag, random);
    }

    public int getNextPieceType() {
        if (currentBag.isEmpty()) {
            currentBag = nextBag;
            nextBag = new ArrayList<>();
            fillNewBag(nextBag);
        }
        return currentBag.remove(0);
    }

    // Change method name back to match original
    public List<Integer> peekNextPieces() {
        List<Integer> preview = new ArrayList<>();
        
        // Take from current bag first
        int fromCurrentBag = Math.min(PREVIEW_SIZE, currentBag.size());
        for (int i = 0; i < fromCurrentBag; i++) {
            preview.add(currentBag.get(i));
        }
        
        // If we need more pieces, ensure nextBag is filled first
        if (preview.size() < PREVIEW_SIZE) {
            if (nextBag.isEmpty()) {
                fillNewBag(nextBag);
            }
            // Take remaining needed pieces from next bag
            int remaining = PREVIEW_SIZE - preview.size();
            for (int i = 0; i < remaining; i++) {
                preview.add(nextBag.get(i));
            }
        }
        
        return preview;
    }
}
