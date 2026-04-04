package view;

import quiz.Question;
import quiz.QuizController;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Main quiz gameplay screen.
 * Shows question, 4 answer option cards, timer, score, and streak.
 * Color feedback: Green = correct, Red = wrong.
 */
public class QuizGameView extends JPanel {

    public interface QuizGameListener {
        void onQuizComplete(QuizController controller);
        void onHome();
    }

    private QuizController controller;
    private QuizGameListener listener;

    // HUD components
    private JLabel questionCountLabel;
    private JLabel scoreLabel;
    private JLabel streakLabel;
    private JLabel timerLabel;
    private JProgressBar progressBar;

    // Question area
    private JLabel questionLabel;
    private JButton[] optionBtns;

    // Explanation area
    private JLabel explanationLabel;
    private JButton nextBtn;

    // Timer
    private Timer countdownTimer;
    private int secondsLeft = 15;

    // State
    private boolean answered = false;

    public QuizGameView(QuizController controller, QuizGameListener listener) {
        this.controller = controller;
        this.listener   = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        buildHUD();
        buildQuestionArea();
        buildOptionsArea();
        buildFeedbackArea();
        showQuestion();
    }

    // ---- HUD ----
    private void buildHUD() {
        JPanel hud = new JPanel(new BorderLayout());
        hud.setBackground(UIConstants.BG_PANEL);
        hud.setBorder(new EmptyBorder(10, 20, 6, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        questionCountLabel = hudLabel("Q 1 / 10");
        scoreLabel         = hudLabel("Score: 0");
        streakLabel        = hudLabel("Streak: 0");
        left.add(questionCountLabel);
        left.add(makeSep());
        left.add(scoreLabel);
        left.add(makeSep());
        left.add(streakLabel);

        JLabel title = new JLabel("Quiz Mode", SwingConstants.CENTER);
        title.setFont(UIConstants.FONT_HEADING);
        title.setForeground(UIConstants.ACCENT_CYAN);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        timerLabel = new JLabel("15s");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        timerLabel.setForeground(UIConstants.SUCCESS_GREEN);
        JButton homeBtn = makeSmallBtn("Home", new Color(60,60,80));
        homeBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                stopTimer();
                listener.onHome();
            }
        });
        right.add(timerLabel);
        right.add(homeBtn);

        hud.add(left,  BorderLayout.WEST);
        hud.add(title, BorderLayout.CENTER);
        hud.add(right, BorderLayout.EAST);

        // Progress bar
        progressBar = new JProgressBar(0, controller.getTotalQuestions());
        progressBar.setValue(0);
        progressBar.setForeground(UIConstants.ACCENT_BLUE);
        progressBar.setBackground(new Color(30,40,70));
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 4));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(hud,         BorderLayout.CENTER);
        topPanel.add(progressBar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);
    }

    // ---- Question Area ----
    private void buildQuestionArea() {
        JPanel qPanel = new JPanel(new GridBagLayout());
        qPanel.setBackground(UIConstants.BG_DARK);
        qPanel.setBorder(new EmptyBorder(20, 40, 10, 40));
        qPanel.setPreferredSize(new Dimension(0, 140));

        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        questionLabel.setForeground(UIConstants.TEXT_PRIMARY);

        qPanel.add(questionLabel);
        add(qPanel, BorderLayout.CENTER);
    }

    // ---- Options Area (2x2 grid of cards) ----
    private void buildOptionsArea() {
        JPanel optPanel = new JPanel(new GridLayout(2, 2, 12, 12));
        optPanel.setBackground(UIConstants.BG_DARK);
        optPanel.setBorder(new EmptyBorder(0, 40, 10, 40));

        optionBtns = new JButton[4];
        String[] prefixes = {"A. ", "B. ", "C. ", "D. "};

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            JButton btn = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),14,14));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setText(prefixes[i]);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            btn.setForeground(UIConstants.TEXT_PRIMARY);
            btn.setBackground(new Color(30,45,80));
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(new EmptyBorder(10, 16, 10, 16));

            btn.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (!answered) onAnswerSelected(idx);
                }
            });

            // Hover effect
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    if (!answered) btn.setBackground(new Color(45,65,110));
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (!answered) btn.setBackground(new Color(30,45,80));
                }
            });

            optionBtns[i] = btn;
            optPanel.add(btn);
        }

        add(optPanel, BorderLayout.SOUTH);
    }

    // ---- Feedback Area ----
    private void buildFeedbackArea() {
        JPanel feedbackPanel = new JPanel(new BorderLayout(0, 6));
        feedbackPanel.setBackground(UIConstants.BG_DARK);
        feedbackPanel.setBorder(new EmptyBorder(4, 40, 12, 40));

        explanationLabel = new JLabel(" ", SwingConstants.CENTER);
        explanationLabel.setFont(UIConstants.FONT_SMALL);
        explanationLabel.setForeground(UIConstants.TEXT_MUTED);

        nextBtn = new JButton("Next Question");
        nextBtn.setFont(UIConstants.FONT_BUTTON);
        nextBtn.setForeground(Color.WHITE);
        nextBtn.setBackground(UIConstants.ACCENT_BLUE);
        nextBtn.setOpaque(true);
        nextBtn.setFocusPainted(false);
        nextBtn.setBorder(BorderFactory.createEmptyBorder(8,24,8,24));
        nextBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextBtn.setVisible(false);
        nextBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { goNext(); }
        });

        JPanel nextRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        nextRow.setOpaque(false);
        nextRow.add(nextBtn);

        feedbackPanel.add(explanationLabel, BorderLayout.CENTER);
        feedbackPanel.add(nextRow,          BorderLayout.SOUTH);

        // Insert between question and options
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UIConstants.BG_DARK);

        // Reassemble center area
        remove(getComponent(1)); // remove old CENTER
        JPanel qArea = new JPanel(new GridBagLayout());
        qArea.setBackground(UIConstants.BG_DARK);
        qArea.setBorder(new EmptyBorder(20,40,6,40));
        qArea.setPreferredSize(new Dimension(0,120));
        qArea.add(questionLabel);

        centerPanel.add(qArea,          BorderLayout.NORTH);
        centerPanel.add(feedbackPanel,  BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    // ---- Question Display ----
    private void showQuestion() {
        Question q = controller.getCurrentQuestion();
        if (q == null) return;

        answered = false;
        secondsLeft = 15;

        // Update HUD
        questionCountLabel.setText("Q " + (controller.getCurrentIndex() + 1)
                + " / " + controller.getTotalQuestions());
        scoreLabel.setText("Score: " + controller.getScore());
        streakLabel.setText("Streak: " + controller.getStreak());
        progressBar.setValue(controller.getCurrentIndex());
        timerLabel.setText("15s");
        timerLabel.setForeground(UIConstants.SUCCESS_GREEN);

        // Show question
        questionLabel.setText("<html><div style='text-align:center;width:600px;'>"
                + q.getQuestion() + "</div></html>");

        // Show options
        String[] prefixes = {"A.  ", "B.  ", "C.  ", "D.  "};
        for (int i = 0; i < 4; i++) {
            optionBtns[i].setText("<html>" + prefixes[i] + q.getOptions()[i] + "</html>");
            optionBtns[i].setBackground(new Color(30,45,80));
            optionBtns[i].setForeground(UIConstants.TEXT_PRIMARY);
        }

        explanationLabel.setText(" ");
        nextBtn.setVisible(false);

        controller.startQuestion();
        startCountdown();
    }

    private void onAnswerSelected(int idx) {
        stopTimer();
        answered = true;
        boolean correct = controller.submitAnswer(idx);
        Question q = controller.getCurrentQuestion() != null
                ? null : null; // already moved to next
        // Show feedback colors
        int correctIdx = getCurrentQuestionCorrectIndex();
        for (int i = 0; i < 4; i++) {
            if (i == correctIdx) {
                optionBtns[i].setBackground(new Color(46,204,113));
                optionBtns[i].setForeground(Color.WHITE);
            } else if (i == idx && !correct) {
                optionBtns[i].setBackground(new Color(231,76,60));
                optionBtns[i].setForeground(Color.WHITE);
            }
        }
        scoreLabel.setText("Score: " + controller.getScore());
        streakLabel.setText("Streak: " + controller.getStreak());

        // Show explanation
        Question prev = getPreviousQuestion();
        if (prev != null) {
            explanationLabel.setForeground(correct
                    ? UIConstants.SUCCESS_GREEN : UIConstants.ERROR_RED);
            explanationLabel.setText(correct ? "Correct! " + prev.getExplanation()
                    : "Wrong! " + prev.getExplanation());
        }

        nextBtn.setText(controller.hasNext() ? "Next Question" : "See Results");
        nextBtn.setVisible(true);
    }

    private int currentCorrectIndex = 0;
    private Question lastQuestion = null;

    private int getCurrentQuestionCorrectIndex() { return currentCorrectIndex; }
    private Question getPreviousQuestion()        { return lastQuestion; }

    private void goNext() {
        if (!controller.hasNext()) {
            stopTimer();
            listener.onQuizComplete(controller);
        } else {
            showQuestion();
        }
    }

    // Override showQuestion to track correct index
    @Override
    public void addNotify() {
        super.addNotify();
        refreshQuestion();
    }

    private void refreshQuestion() {
        Question q = controller.getCurrentQuestion();
        if (q != null) {
            currentCorrectIndex = q.getCorrectIndex();
            lastQuestion = q;
        }
    }

    // ---- Timer ----
    private void startCountdown() {
        stopTimer();
        refreshQuestion();
        countdownTimer = new Timer(1000, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                secondsLeft--;
                timerLabel.setText(secondsLeft + "s");
                if (secondsLeft <= 5) timerLabel.setForeground(UIConstants.ERROR_RED);
                else if (secondsLeft <= 10) timerLabel.setForeground(new Color(241,196,15));
                if (secondsLeft <= 0) {
                    stopTimer();
                    if (!answered) onAnswerSelected(-1); // time up = wrong
                }
            }
        });
        countdownTimer.start();
    }

    private void stopTimer() {
        if (countdownTimer != null) countdownTimer.stop();
    }

    // ---- Helpers ----
    private JLabel hudLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(UIConstants.FONT_BODY);
        l.setForeground(UIConstants.TEXT_PRIMARY);
        return l;
    }
    private JSeparator makeSep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1,18));
        s.setForeground(new Color(60,80,120));
        return s;
    }
    private JButton makeSmallBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(UIConstants.FONT_SMALL);
        btn.setForeground(UIConstants.TEXT_PRIMARY);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(4,10,4,10));
        return btn;
    }
}