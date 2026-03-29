package model;

/**
 * Represents a single card on the game board.
 * Each card has a value (its matching symbol), a category,
 * and state flags for flipped/matched status.
 */
public class Card {

    /** Categories supported for card faces */
    public enum Category {
        SHAPES, ANIMALS, FRUITS, EMOJIS
    }

    private String value;       // The symbol or name that pairs cards
    private Category category;
    private boolean flipped;    // Currently face-up?
    private boolean matched;    // Successfully paired?
    private int row;
    private int col;

    public Card(String value, Category category, int row, int col) {
        this.value = value;
        this.category = category;
        this.row = row;
        this.col = col;
        this.flipped = false;
        this.matched = false;
    }

    // Getters
    public String getValue() { return value; }
    public Category getCategory() { return category; }
    public boolean isFlipped() { return flipped; }
    public boolean isMatched() { return matched; }
    public int getRow() { return row; }
    public int getCol() { return col; }

    // Setters
    public void setFlipped(boolean flipped) { this.flipped = flipped; }
    public void setMatched(boolean matched) {
        this.matched = matched;
        if (matched) this.flipped = true; // Matched cards stay face-up
    }

    /**
     * Flip this card face-up (reveal it).
     */
    public void flip() { this.flipped = true; }

    /**
     * Flip this card face-down (hide it).
     */
    public void hide() {
        if (!matched) this.flipped = false;
    }

    /**
     * Check if this card matches another card.
     */
    public boolean matches(Card other) {
        return this.value.equals(other.value) && this.category == other.category;
    }

    @Override
    public String toString() {
        return "Card{value='" + value + "', category=" + category
                + ", flipped=" + flipped + ", matched=" + matched + "}";
    }
}