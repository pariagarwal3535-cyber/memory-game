package model;

import util.ImageLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the game board (grid of cards) for a given level.
 * Cards are identified by image filenames (e.g. "cat.png").
 * Images are loaded from resources/images/<category>/
 *
 * Grid sizes by level:
 *  1->2x2, 2->2x3, 3->2x4, 4->3x4, 5->4x4,
 *  6->4x5, 7->4x6, 8->5x6, 9->6x6, 10->6x7
 */
public class GameBoard {

    private Card[][] grid;
    private int rows;
    private int cols;
    private int totalPairs;
    private int matchedPairs;

    public GameBoard(int level, Card.Category category) {
        int[] dimensions = getDimensions(level);
        this.rows         = dimensions[0];
        this.cols         = dimensions[1];
        this.totalPairs   = (rows * cols) / 2;
        this.matchedPairs = 0;
        this.grid         = new Card[rows][cols];
        initializeBoard(category);
    }

    public static int[] getDimensions(int level) {
        switch (level) {
            case 1:  return new int[]{2, 2};
            case 2:  return new int[]{2, 3};
            case 3:  return new int[]{2, 4};
            case 4:  return new int[]{3, 4};
            case 5:  return new int[]{4, 4};
            case 6:  return new int[]{4, 5};
            case 7:  return new int[]{4, 6};
            case 8:  return new int[]{5, 6};
            case 9:  return new int[]{6, 6};
            case 10: return new int[]{6, 7};
            default: return new int[]{4, 4};
        }
    }

    public static int getPreviewTime(int level) {
        return Math.max(800, 3200 - (level - 1) * 250);
    }

    private void initializeBoard(Card.Category category) {
        String categoryName = category.name().toLowerCase();
        String[] allImages  = ImageLoader.getImageNames(categoryName);

        String[] selected = new String[totalPairs];
        for (int i = 0; i < totalPairs; i++) {
            selected[i] = allImages[i % allImages.length];
        }

        List<Card> cardList = new ArrayList<Card>();
        for (int i = 0; i < totalPairs; i++) {
            cardList.add(new Card(selected[i], category, 0, 0));
            cardList.add(new Card(selected[i], category, 0, 0));
        }

        Collections.shuffle(cardList);

        int index = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Card card = cardList.get(index++);
                grid[r][c] = new Card(card.getValue(), card.getCategory(), r, c);
            }
        }
    }

    public Card getCard(int row, int col) { return grid[row][col]; }
    public int getRows()         { return rows; }
    public int getCols()         { return cols; }
    public int getTotalPairs()   { return totalPairs; }
    public int getMatchedPairs() { return matchedPairs; }

    public void registerMatch(int r1, int c1, int r2, int c2) {
        grid[r1][c1].setMatched(true);
        grid[r2][c2].setMatched(true);
        matchedPairs++;
    }

    public boolean isComplete() {
        return matchedPairs >= totalPairs;
    }

    public void hideUnmatched() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (!grid[r][c].isMatched())
                    grid[r][c].hide();
    }

    public void revealAll() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                grid[r][c].setFlipped(true);
    }

    public void coverAll() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (!grid[r][c].isMatched())
                    grid[r][c].setFlipped(false);
    }
}