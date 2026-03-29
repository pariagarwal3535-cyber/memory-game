package view;

import model.Card;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Allows the player to choose a card category and starting level
 * before launching a game.
 */
public class CategorySelectionView extends JPanel {

    public interface SelectionListener {
        void onSelectionConfirmed(Card.Category category, int level);
        void onBack();
    }

    private SelectionListener listener;
    private Card.Category selectedCategory = Card.Category.EMOJIS;
    private int selectedLevel = 1;

    private JButton[] categoryBtns;
    private JButton[] levelBtns;

    public CategorySelectionView(SelectionListener listener) {
        this.listener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        // Outer wrapper to center content
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_DARK);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(30, 50, 30, 50));

        // Title
        JLabel title = new JLabel("Choose Your Game");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Category section label
        JLabel catLabel = new JLabel("Card Category");
        catLabel.setFont(UIConstants.FONT_HEADING);
        catLabel.setForeground(UIConstants.TEXT_PRIMARY);
        catLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Level section label
        JLabel lvlLabel = new JLabel("Starting Level");
        lvlLabel.setFont(UIConstants.FONT_HEADING);
        lvlLabel.setForeground(UIConstants.TEXT_PRIMARY);
        lvlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Category buttons panel
        JPanel categoryPanel = buildCategoryPanel();
        categoryPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Level buttons panel
        JPanel levelPanel = buildLevelPanel();
        levelPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backBtn = buildToggleButton("  Back  ");
        backBtn.setBackground(new Color(60, 60, 80));
        backBtn.setPreferredSize(new Dimension(140, 44));
        backBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.onBack();
            }
        });

        JButton startBtn = buildToggleButton("Start Game");
        startBtn.setBackground(UIConstants.ACCENT_BLUE);
        startBtn.setBorder(BorderFactory.createLineBorder(UIConstants.ACCENT_CYAN, 2));
        startBtn.setPreferredSize(new Dimension(160, 44));
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.onSelectionConfirmed(selectedCategory, selectedLevel);
            }
        });

        actions.add(backBtn);
        actions.add(startBtn);

        // Assemble
        box.add(title);
        box.add(Box.createVerticalStrut(28));
        box.add(catLabel);
        box.add(Box.createVerticalStrut(10));
        box.add(categoryPanel);
        box.add(Box.createVerticalStrut(24));
        box.add(lvlLabel);
        box.add(Box.createVerticalStrut(10));
        box.add(levelPanel);
        box.add(Box.createVerticalStrut(30));
        box.add(actions);

        center.add(box);
        add(center, BorderLayout.CENTER);
    }

    private JPanel buildCategoryPanel() {
        // Category names without emojis to avoid encoding issues
        final String[] labels   = { "Emojis", "Animals", "Fruits", "Shapes" };
        final Card.Category[] cats = {
            Card.Category.EMOJIS,
            Card.Category.ANIMALS,
            Card.Category.FRUITS,
            Card.Category.SHAPES
        };

        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        p.setOpaque(false);
        categoryBtns = new JButton[labels.length];

        for (int i = 0; i < labels.length; i++) {
            final Card.Category cat = cats[i];
            final int idx = i;
            JButton btn = buildToggleButton(labels[i]);
            btn.setPreferredSize(new Dimension(120, 44));
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedCategory = cat;
                    highlightToggle(categoryBtns, idx);
                }
            });
            categoryBtns[i] = btn;
            p.add(btn);
        }

        highlightToggle(categoryBtns, 0);
        return p;
    }

    private JPanel buildLevelPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        p.setOpaque(false);
        levelBtns = new JButton[10];

        for (int lv = 1; lv <= 10; lv++) {
            final int level = lv;
            JButton btn = buildToggleButton(String.valueOf(lv));
            btn.setPreferredSize(new Dimension(56, 42));
            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedLevel = level;
                    highlightToggle(levelBtns, level - 1);
                }
            });
            levelBtns[lv - 1] = btn;
            p.add(btn);
        }

        highlightToggle(levelBtns, 0);
        return p;
    }

    private JButton buildToggleButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_BUTTON);
        btn.setForeground(UIConstants.TEXT_PRIMARY);
        btn.setBackground(new Color(40, 55, 95));
        btn.setBorder(BorderFactory.createLineBorder(new Color(64, 80, 130), 1));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void highlightToggle(JButton[] btns, int selectedIdx) {
        for (int i = 0; i < btns.length; i++) {
            if (i == selectedIdx) {
                btns[i].setBackground(UIConstants.ACCENT_BLUE);
                btns[i].setBorder(BorderFactory.createLineBorder(UIConstants.ACCENT_CYAN, 2));
            } else {
                btns[i].setBackground(new Color(40, 55, 95));
                btns[i].setBorder(BorderFactory.createLineBorder(new Color(64, 80, 130), 1));
            }
        }
    }
}