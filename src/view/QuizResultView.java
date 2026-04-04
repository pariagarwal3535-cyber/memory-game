package view;

import quiz.QuizController;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Quiz results screen shown after all questions are answered.
 * Shows score, accuracy, streak, time, and grade.
 */
public class QuizResultView extends JPanel {

    public interface ResultListener {
        void onPlayAgain();
        void onHome();
    }

    public QuizResultView(QuizController controller, ResultListener listener) {
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI(controller, listener);
    }

    private void buildUI(QuizController ctrl, ResultListener listener) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(30, 60, 30, 60));

        // Grade calculation
        double accuracy = ctrl.getAccuracy();
        String grade    = getGrade(accuracy);
        Color gradeColor = getGradeColor(accuracy);

        // Title
        JLabel trophy = new JLabel(getTrophy(accuracy), SwingConstants.CENTER);
        trophy.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        trophy.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Quiz Complete!", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel gradeLbl = new JLabel("Grade: " + grade, SwingConstants.CENTER);
        gradeLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        gradeLbl.setForeground(gradeColor);
        gradeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Stats grid
        JPanel stats = new JPanel(new GridLayout(4, 2, 20, 10));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.CENTER_ALIGNMENT);
        stats.setMaximumSize(new Dimension(360, 160));

        addStat(stats, "Final Score",   String.valueOf(ctrl.getScore()));
        addStat(stats, "Correct",       ctrl.getCorrect() + " / " + ctrl.getTotalQuestions());
        addStat(stats, "Accuracy",      String.format("%.1f%%", accuracy));
        addStat(stats, "Best Streak",   String.valueOf(ctrl.getMaxStreak()));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(50,70,110));
        sep.setMaximumSize(new Dimension(360, 2));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btns.setOpaque(false);
        btns.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton againBtn = makeBtn("Play Again", UIConstants.ACCENT_BLUE);
        JButton homeBtn  = makeBtn("Home",       new Color(60,60,80));

        againBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onPlayAgain(); }
        });
        homeBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onHome(); }
        });

        btns.add(againBtn);
        btns.add(homeBtn);

        box.add(trophy);
        box.add(Box.createVerticalStrut(6));
        box.add(title);
        box.add(Box.createVerticalStrut(4));
        box.add(gradeLbl);
        box.add(Box.createVerticalStrut(20));
        box.add(stats);
        box.add(Box.createVerticalStrut(16));
        box.add(sep);
        box.add(Box.createVerticalStrut(20));
        box.add(btns);

        add(box);
    }

    private void addStat(JPanel p, String label, String value) {
        JLabel lbl = new JLabel(label, SwingConstants.RIGHT);
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 16));
        val.setForeground(UIConstants.ACCENT_CYAN);

        p.add(lbl);
        p.add(val);
    }

    private String getGrade(double accuracy) {
        if (accuracy >= 90) return "A+";
        if (accuracy >= 80) return "A";
        if (accuracy >= 70) return "B";
        if (accuracy >= 60) return "C";
        if (accuracy >= 50) return "D";
        return "F";
    }

    private Color getGradeColor(double accuracy) {
        if (accuracy >= 80) return new Color(46,204,113);
        if (accuracy >= 60) return new Color(241,196,15);
        return new Color(231,76,60);
    }

    private String getTrophy(double accuracy) {
        if (accuracy >= 90) return "\uD83C\uDFC6";
        if (accuracy >= 70) return "\uD83C\uDF1F";
        if (accuracy >= 50) return "\uD83D\uDC4D";
        return "\uD83D\uDCDA";
    }

    private JButton makeBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10,24,10,24));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}