package view;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import model.Card;
import util.ImageLoader;
import util.UIConstants;

/**
 * A single card on the game board.
 * Supports dynamic resizing via setCardSize().
 * Shows real emoji/images on face-up cards.
 * Shows decorated back on face-down cards.
 */
public class CardView extends JButton {

    private Card card;
    private int  size;

    public CardView(Card card, int size) {
        this.card = card;
        this.size = size;
        applySize();
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Called by GameBoardView when window is resized */
    public void setCardSize(int newSize) {
        if (this.size != newSize) {
            this.size = newSize;
            applySize();
            revalidate();
            repaint();
        }
    }

    private void applySize() {
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    public Card getCard() { return card; }

    public void update() { repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        int w   = getWidth();
        int h   = getHeight();
        int arc = 16;

        if (card.isMatched()) {
            // Matched — green background with image
            paintBackground(g2, UIConstants.CARD_MATCHED, UIConstants.CARD_MATCHED.darker(), w, h, arc);
            drawCardImage(g2, w, h, 8);

        } else if (card.isFlipped()) {
            // Face-up — white background with image
            paintBackground(g2, UIConstants.CARD_FACE, UIConstants.ACCENT_BLUE, w, h, arc);
            drawCardImage(g2, w, h, 8);

        } else {
            // Face-down — dark blue with decorative pattern
            paintBack(g2, w, h, arc);
        }

        g2.dispose();
    }

    private void paintBackground(Graphics2D g2, Color fill, Color border,
                                  int w, int h, int arc) {
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
    }

    private void paintBack(Graphics2D g2, int w, int h, int arc) {
        // Dark blue base
        g2.setColor(UIConstants.CARD_BACK);
        g2.fill(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));

        // Subtle diagonal stripe pattern
        g2.setColor(new Color(50, 85, 150, 55));
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = -h; i < w + h; i += 16) {
            g2.drawLine(i, 0, i + h, h);
        }

        // Inner rounded border
        g2.setColor(new Color(70, 110, 180, 120));
        g2.setStroke(new BasicStroke(1.5f));
        int pad = 8;
        g2.draw(new RoundRectangle2D.Float(pad, pad, w-pad*2, h-pad*2, arc/2, arc/2));

        // Question mark in center
        g2.setColor(new Color(100, 145, 220));
        int fontSize = Math.max(18, (int)(h * 0.38));
        g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        String qm = "?";
        int tx = (w - fm.stringWidth(qm)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(qm, tx, ty);

        // Outer border
        g2.setColor(new Color(60, 95, 165));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(new RoundRectangle2D.Float(2, 2, w-4, h-4, arc, arc));
    }

    /**
     * Draw the card image/emoji centered on the card.
     * Uses ImageLoader which tries real files first, then built-in emojis.
     */
    private void drawCardImage(Graphics2D g2, int w, int h, int pad) {
        String category = card.getCategory().name().toLowerCase();
        String filename  = card.getValue();
        int    imgSize   = Math.min(w, h) - (pad * 2);
        if (imgSize < 10) return;

        ImageIcon icon = ImageLoader.loadImage(category, filename, imgSize);
        if (icon != null && icon.getIconWidth() > 0) {
            int x = (w - icon.getIconWidth())  / 2;
            int y = (h - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, x, y);
        }
    }
}