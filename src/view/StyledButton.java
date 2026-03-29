package view;

import util.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A custom rounded button used throughout the game UI.
 */
public class StyledButton extends JButton {

    private Color baseColor;
    private Color hoverColor;
    private boolean hovered = false;

    public StyledButton(String text) {
        this(text, UIConstants.BUTTON_BG, UIConstants.BUTTON_HOVER);
    }

    public StyledButton(String text, Color base, Color hover) {
        super(text);
        this.baseColor  = base;
        this.hoverColor = hover;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.WHITE);
        setFont(UIConstants.FONT_BUTTON);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(160, 42));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            @Override public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = hovered ? hoverColor : baseColor;
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

        // Subtle gradient overlay
        GradientPaint gp = new GradientPaint(0, 0, new Color(255,255,255,40),
                0, getHeight(), new Color(0,0,0,30));
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));

        g2.dispose();
        super.paintComponent(g);
    }
}
