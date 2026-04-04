package view;

import quiz.Question;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Subject and difficulty selection screen for Quiz Mode.
 */
public class QuizSelectionView extends JPanel {

    public interface QuizSelectionListener {
        void onStartQuiz(Question.Subject subject, Question.Difficulty difficulty);
        void onBack();
    }

    private QuizSelectionListener listener;
    private Question.Subject    selectedSubject    = Question.Subject.GK;
    private Question.Difficulty selectedDifficulty = Question.Difficulty.EASY;
    private JButton[] subjectBtns;
    private JButton[] diffBtns;

    public QuizSelectionView(QuizSelectionListener listener) {
        this.listener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_DARK);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel title = new JLabel("Quiz Mode");
        title.setFont(new Font("Segoe UI", Font.BOLD, 38));
        title.setForeground(UIConstants.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Test your knowledge across subjects");
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subjectLbl = sectionLabel("Choose Subject");
        JPanel subjectPanel = buildSubjectPanel();
        subjectPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel diffLbl = sectionLabel("Choose Difficulty");
        JPanel diffPanel = buildDiffPanel();
        diffPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel info = new JLabel("10 questions | Score based on speed + streak");
        info.setFont(UIConstants.FONT_SMALL);
        info.setForeground(UIConstants.TEXT_MUTED);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backBtn  = makeBtn("Back",       new Color(60,60,80));
        JButton startBtn = makeBtn("Start Quiz", UIConstants.ACCENT_BLUE);
        startBtn.setPreferredSize(new Dimension(150, 44));

        backBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { listener.onBack(); }
        });
        startBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                listener.onStartQuiz(selectedSubject, selectedDifficulty);
            }
        });

        actions.add(backBtn);
        actions.add(startBtn);

        box.add(title);
        box.add(Box.createVerticalStrut(4));
        box.add(sub);
        box.add(Box.createVerticalStrut(20));
        box.add(subjectLbl);
        box.add(Box.createVerticalStrut(10));
        box.add(subjectPanel);
        box.add(Box.createVerticalStrut(18));
        box.add(diffLbl);
        box.add(Box.createVerticalStrut(10));
        box.add(diffPanel);
        box.add(Box.createVerticalStrut(12));
        box.add(info);
        box.add(Box.createVerticalStrut(22));
        box.add(actions);

        center.add(box);
        add(center, BorderLayout.CENTER);
    }

    private JPanel buildSubjectPanel() {
        final String[][] subjects = {
            {"GK",   "General Knowledge",  "GK"},
            {"ENG",  "English Grammar",    "ENGLISH"},
            {"OS",   "Operating Systems",  "OPERATING_SYSTEMS"},
            {"DSA",  "Data Structures",    "DATA_STRUCTURES"},
            {"NET",  "Networks",           "COMPUTER_NETWORKS"},
            {"DBMS", "DBMS",               "DBMS"},
            {"OOP",  "OOP Concepts",       "OOP"},
            {"ALGO", "Algorithms",         "ALGORITHMS"}
        };

        JPanel p = new JPanel(new GridLayout(2, 4, 8, 8));
        p.setOpaque(false);
        subjectBtns = new JButton[subjects.length];

        for (int i = 0; i < subjects.length; i++) {
            final String subjectName = subjects[i][2];
            final int idx = i;
            JButton btn = new JButton("<html><center><b>" + subjects[i][0]
                    + "</b><br><small>" + subjects[i][1] + "</small></center></html>");
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btn.setForeground(UIConstants.TEXT_PRIMARY);
            btn.setBackground(new Color(35,50,85));
            btn.setFocusPainted(false);
            btn.setOpaque(true);
            btn.setBorder(BorderFactory.createLineBorder(new Color(60,80,130), 1));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(140, 56));
            btn.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    selectedSubject = Question.Subject.valueOf(subjectName);
                    highlightBtns(subjectBtns, idx);
                }
            });
            subjectBtns[i] = btn;
            p.add(btn);
        }
        highlightBtns(subjectBtns, 0);
        return p;
    }

    private JPanel buildDiffPanel() {
        final String[] labels = {"Easy", "Medium", "Hard"};
        final Color[] colors = {
            new Color(46,204,113), new Color(241,196,15), new Color(231,76,60)
        };
        final Question.Difficulty[] diffs = {
            Question.Difficulty.EASY, Question.Difficulty.MEDIUM, Question.Difficulty.HARD
        };
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        p.setOpaque(false);
        diffBtns = new JButton[3];

        for (int i = 0; i < 3; i++) {
            final Question.Difficulty diff = diffs[i];
            final Color col = colors[i];
            final int idx = i;
            JButton btn = makeBtn(labels[i], new Color(40,55,80));
            btn.setPreferredSize(new Dimension(120, 44));
            btn.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    selectedDifficulty = diff;
                    for (int j = 0; j < diffBtns.length; j++)
                        diffBtns[j].setBackground(j == idx ? col : new Color(40,55,80));
                }
            });
            diffBtns[i] = btn;
            p.add(btn);
        }
        diffBtns[0].setBackground(colors[0]);
        return p;
    }

    private JButton makeBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_BUTTON);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8,18,8,18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_HEADING);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private void highlightBtns(JButton[] btns, int selected) {
        for (int i = 0; i < btns.length; i++) {
            btns[i].setBackground(i == selected ? UIConstants.ACCENT_BLUE : new Color(35,50,85));
            btns[i].setBorder(BorderFactory.createLineBorder(
                    i == selected ? UIConstants.ACCENT_CYAN : new Color(60,80,130),
                    i == selected ? 2 : 1));
        }
    }
}