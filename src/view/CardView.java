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
 * Renders card back (face-down) or card image (face-up).
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

    /** Repaint this card */
    public void update() {
        repaint();
    }

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
        int arc = 12;

        if (card.isMatched()) {
            // Green — matched pair
            g2.setColor(UIConstants.CARD_MATCHED);
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, arc, arc));
            // Green border
            g2.setColor(UIConstants.CARD_MATCHED.darker());
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, arc, arc));
            drawImage(g2, w, h);

        } else if (card.isFlipped()) {
            // White — face up / revealed
            g2.setColor(UIConstants.CARD_FACE);
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, arc, arc));
            g2.setColor(UIConstants.ACCENT_BLUE);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, arc, arc));
            drawImage(g2, w, h);

        } else {
            // Dark blue — face down
            g2.setColor(UIConstants.CARD_BACK);
            g2.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, arc, arc));

            // Diagonal stripe pattern on back
            g2.setColor(new Color(50, 80, 140, 60));
            g2.setStroke(new BasicStroke(1.5f));
            for (int i = -h; i < w + h; i += 14) {
                g2.drawLine(i, 0, i + h, h);
            }

            // "?" label
            g2.setColor(new Color(90, 130, 210));
            int fontSize = Math.max(12, (int) (h * 0.36));
            g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            String qm = "?";
            int tx = (w - fm.stringWidth(qm)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(qm, tx, ty);

            // Border
            g2.setColor(new Color(60, 90, 160));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, arc, arc));
        }

        g2.dispose();
    }

    /** Draw the card's image centered within the card bounds */
    private void drawImage(Graphics2D g2, int w, int h) {
        String category = card.getCategory().name().toLowerCase();
        String filename  = card.getValue();
        int    pad      = 6;
        int    imgSize  = Math.min(w, h) - (pad * 2);
        if (imgSize < 10) return;

        ImageIcon icon = ImageLoader.loadImage(category, filename, imgSize);
        if (icon != null && icon.getIconWidth() > 0) {
            int x = (w - icon.getIconWidth())  / 2;
            int y = (h - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g2, x, y);
        }
    }
}