package view;

import network.GameClient;
import util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Multiplayer quiz screen.
 *
 * Layout:
 *   - Top:    Title + Home button + scoreboard pills (updates live)
 *   - Center: Question, 4 option buttons, Buzz button
 *   - Bottom: Turn indicator, phase label, countdown
 *
 * Phase model mirrors the server:
 *   Phase 1 - primary player answers (others disabled)
 *   Phase 2 - steal; non-primary players can click Buzz. First buzzer
 *             gets the 4 options enabled, others are locked out.
 */
public class MultiplayerQuizView extends JPanel
        implements GameClient.MessageListener {

    public interface MPQuizListener { void onHomeClicked(); }

    private final GameClient client;
    private final String roomId;
    private final String myUsername;
    private final int totalQuestions;
    private final MPQuizListener listener;

    // --- Header widgets ---
    private JLabel questionLabel;
    private JLabel progressLabel;      // "Question 3 / 10"
    private JLabel turnLabel;          // "Your question" / "Alex's question"
    private JLabel phaseLabel;         // "Steal round! Buzz to answer"
    private JLabel timerLabel;         // "12s"
    private JLabel feedbackLabel;      // "Correct! +5" or "Wrong -2"
    private JPanel scoresPanel;

    // --- Center ---
    private JButton[] optionButtons = new JButton[4];
    private JButton   buzzButton;

    // --- State ---
    private String currentPrimary = "";
    private int    currentPhase   = 0;       // 0 idle, 1 primary, 2 steal
    private String currentBuzzer  = null;
    private int    correctIndexRevealed = -1; // for post-answer highlight
    private int    secondsLeft;
    private Timer  countdownTimer;

    public MultiplayerQuizView(String roomId, String myUsername,
                                GameClient client, int totalQuestions,
                                String scoreboard, String firstTurn,
                                MPQuizListener listener) {
        this.roomId = roomId;
        this.myUsername = myUsername;
        this.client = client;
        this.totalQuestions = totalQuestions;
        this.listener = listener;
        this.currentPrimary = firstTurn;

        client.setListener(this);

        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildHeader(scoreboard);
        buildCenter();
        buildFooter();

        setQuestionEnabled(false);
        updateTurnLabel();
    }

    // =============== Layout ===============

    private void buildHeader(String scoreboard) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_PANEL);
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Multiplayer Quiz", SwingConstants.LEFT);
        title.setFont(UIConstants.FONT_HEADING);
        title.setForeground(UIConstants.ACCENT_CYAN);

        JButton homeBtn = new JButton("Home");
        homeBtn.setFont(UIConstants.FONT_SMALL);
        homeBtn.setBackground(new Color(60,60,80));
        homeBtn.setForeground(UIConstants.TEXT_PRIMARY);
        homeBtn.setFocusPainted(false);
        homeBtn.setOpaque(true);
        homeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        homeBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                client.quit(roomId, myUsername);
                client.disconnect();
                listener.onHomeClicked();
            }
        });

        top.add(title, BorderLayout.WEST);
        top.add(homeBtn, BorderLayout.EAST);

        scoresPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        scoresPanel.setOpaque(false);
        scoresPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        rebuildScoresPanel(scoreboard);

        header.add(top, BorderLayout.NORTH);
        header.add(scoresPanel, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
    }

    private void rebuildScoresPanel(String scoreboard) {
        scoresPanel.removeAll();
        if (scoreboard == null || scoreboard.isEmpty()) {
            scoresPanel.revalidate();
            scoresPanel.repaint();
            return;
        }

        // Header label
        JLabel lbl = new JLabel("SCORES:");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        scoresPanel.add(lbl);

        // Format: user|score|color~user|score|color
        String[] entries = scoreboard.split("~");
        for (String entry : entries) {
            String[] parts = entry.split("\\|");
            if (parts.length < 3) continue;
            final String pName  = parts[0];
            final String pScore = parts[1];
            final String pColor = parts[2];

            Color decoded;
            try { decoded = Color.decode(pColor); }
            catch (Exception ex) { decoded = Color.WHITE; }
            final Color color = decoded;

            JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            pill.setOpaque(true);
            pill.setBackground(new Color(color.getRed(), color.getGreen(),
                    color.getBlue(), 40));
            int borderThickness = pName.equals(currentPrimary) ? 3 : 2;
            pill.setBorder(BorderFactory.createLineBorder(color, borderThickness));

            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(color);
                    g.fillOval(2, 2, getWidth()-4, getHeight()-4);
                }
            };
            dot.setPreferredSize(new Dimension(12, 12));
            dot.setOpaque(false);

            JLabel nameLbl = new JLabel(pName
                    + (pName.equals(myUsername) ? " (you)" : "")
                    + ": " + pScore);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            nameLbl.setForeground(
                    pName.equals(myUsername) ? color : UIConstants.TEXT_PRIMARY);

            pill.add(dot);
            pill.add(nameLbl);

            // Crown for leader
            scoresPanel.add(pill);
        }
        scoresPanel.revalidate();
        scoresPanel.repaint();
    }

    private void buildCenter() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_DARK);
        center.setBorder(new EmptyBorder(16, 32, 16, 32));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UIConstants.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60,70,95), 1),
                new EmptyBorder(20, 28, 20, 28)));

        progressLabel = new JLabel("Waiting to start...", SwingConstants.CENTER);
        progressLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        progressLabel.setForeground(UIConstants.TEXT_MUTED);
        progressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        questionLabel = new JLabel(" ", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        questionLabel.setForeground(UIConstants.TEXT_PRIMARY);
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel optionsGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        optionsGrid.setOpaque(false);
        optionsGrid.setMaximumSize(new Dimension(700, 220));
        optionsGrid.setPreferredSize(new Dimension(700, 220));

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            JButton b = makeOptionButton("Option " + (char)('A'+i));
            b.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    onOptionClick(idx);
                }
            });
            optionButtons[i] = b;
            optionsGrid.add(b);
        }

        buzzButton = new JButton("BUZZ!");
        buzzButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        buzzButton.setForeground(Color.WHITE);
        buzzButton.setBackground(new Color(220, 80, 50));
        buzzButton.setOpaque(true);
        buzzButton.setFocusPainted(false);
        buzzButton.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        buzzButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buzzButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        buzzButton.setEnabled(false);
        buzzButton.setVisible(false);
        buzzButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                onBuzzClick();
            }
        });

        feedbackLabel = new JLabel(" ", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(progressLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(questionLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(optionsGrid);
        card.add(Box.createVerticalStrut(14));
        card.add(buzzButton);
        card.add(Box.createVerticalStrut(10));
        card.add(feedbackLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        center.add(card, gbc);

        add(center, BorderLayout.CENTER);
    }

    private JButton makeOptionButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getBackground();
                if (!isEnabled()) bg = bg.darker();
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 15));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(55, 95, 160));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.CENTER);
        return b;
    }

    private void buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIConstants.BG_PANEL);
        footer.setBorder(new EmptyBorder(8, 16, 8, 16));

        turnLabel = new JLabel(" ", SwingConstants.LEFT);
        turnLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        turnLabel.setForeground(UIConstants.TEXT_PRIMARY);

        phaseLabel = new JLabel(" ", SwingConstants.CENTER);
        phaseLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        phaseLabel.setForeground(UIConstants.TEXT_MUTED);

        timerLabel = new JLabel(" ", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(UIConstants.ACCENT_CYAN);

        footer.add(turnLabel, BorderLayout.WEST);
        footer.add(phaseLabel, BorderLayout.CENTER);
        footer.add(timerLabel, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }

    // =============== User actions ===============

    private boolean answerSent = false;  // guard against double-clicks on the same question

    private void onOptionClick(int idx) {
        if (answerSent) return;
        if (currentPhase == 1 && myUsername.equals(currentPrimary)) {
            answerSent = true;
            setQuestionEnabled(false);
            client.sendAnswer(roomId, myUsername, idx);
        } else if (currentPhase == 2 && myUsername.equals(currentBuzzer)) {
            answerSent = true;
            setQuestionEnabled(false);
            client.sendAnswer(roomId, myUsername, idx);
        }
    }

    private void onBuzzClick() {
        if (currentPhase == 2 && !myUsername.equals(currentPrimary)
                && currentBuzzer == null) {
            buzzButton.setEnabled(false);
            buzzButton.setText("Buzzed!");
            client.sendBuzz(roomId, myUsername);
        }
    }

    // =============== Helpers ===============

    private void updateTurnLabel() {
        if (currentPrimary == null || currentPrimary.isEmpty()) {
            turnLabel.setText("  Waiting...");
            return;
        }
        if (myUsername.equals(currentPrimary)) {
            turnLabel.setText("  YOUR QUESTION");
            turnLabel.setForeground(UIConstants.SUCCESS_GREEN);
        } else {
            turnLabel.setText("  " + currentPrimary + "'s question");
            turnLabel.setForeground(UIConstants.TEXT_MUTED);
        }
    }

    private void setQuestionEnabled(boolean enabled) {
        for (JButton b : optionButtons) b.setEnabled(enabled);
    }

    private void resetOptionColors() {
        for (JButton b : optionButtons) {
            b.setBackground(new Color(55, 95, 160));
        }
    }

    private void showPhase1(String primary, String question,
                             String[] options, int seconds) {
        currentPhase = 1;
        currentBuzzer = null;
        correctIndexRevealed = -1;
        currentPrimary = primary;
        answerSent = false;          // new question - re-allow answering

        questionLabel.setText("<html><div style='text-align:center;'>"
                + escape(question) + "</div></html>");

        for (int i = 0; i < 4; i++) {
            String label = "<html><div style='text-align:center;'>"
                    + (char)('A'+i) + ".  " + escape(options[i])
                    + "</div></html>";
            optionButtons[i].setText(label);
        }
        resetOptionColors();

        buzzButton.setVisible(false);
        buzzButton.setEnabled(false);
        buzzButton.setText("BUZZ!");

        feedbackLabel.setText(" ");
        phaseLabel.setText("Answer within " + seconds + "s");

        boolean iAmPrimary = myUsername.equals(currentPrimary);
        setQuestionEnabled(iAmPrimary);
        updateTurnLabel();
        startCountdown(seconds);
    }

    private void showPhase2(int seconds) {
        currentPhase = 2;
        currentBuzzer = null;
        answerSent = false;          // steal round - new answer opportunity

        boolean iAmPrimary = myUsername.equals(currentPrimary);
        setQuestionEnabled(false);  // disabled until someone buzzes
        buzzButton.setVisible(true);
        buzzButton.setEnabled(!iAmPrimary);
        buzzButton.setText(iAmPrimary ? "You're out this round" : "BUZZ!");

        phaseLabel.setText(
                iAmPrimary
                    ? "Steal round - you cannot buzz"
                    : "Steal round! First to buzz gets to answer (" + seconds + "s)");

        startCountdown(seconds);
    }

    private void onBuzzedMessage(String buzzer, int seconds) {
        currentBuzzer = buzzer;
        buzzButton.setEnabled(false);
        buzzButton.setText(buzzer.equals(myUsername) ? "You buzzed!" : buzzer + " buzzed!");

        if (myUsername.equals(buzzer)) {
            // Buzzer gets options enabled
            setQuestionEnabled(true);
            phaseLabel.setText("You have the floor - answer within " + seconds + "s");
        } else {
            setQuestionEnabled(false);
            phaseLabel.setText(buzzer + " is answering...");
        }
        startCountdown(seconds);
    }

    private void onAnswered(String user, int optIdx, boolean correct,
                             int correctIdx) {
        stopCountdown();
        correctIndexRevealed = correctIdx;

        // Only paint the board on FINAL resolution (correctIdx >= 0).
        // If correctIdx == -1 the question is going to a steal round -
        // leave the buttons neutral so the next player sees a clean slate.
        if (correctIdx >= 0 && correctIdx < 4) {
            optionButtons[correctIdx].setBackground(new Color(35, 160, 95));
            if (optIdx >= 0 && optIdx < 4 && optIdx != correctIdx) {
                optionButtons[optIdx].setBackground(new Color(180, 55, 55));
            }
        }

        setQuestionEnabled(false);
        buzzButton.setEnabled(false);

        if (optIdx < 0) {
            feedbackLabel.setText(user + " ran out of time");
            feedbackLabel.setForeground(UIConstants.TEXT_MUTED);
        } else if (correct) {
            feedbackLabel.setText(user + " is correct!  +5");
            feedbackLabel.setForeground(UIConstants.SUCCESS_GREEN);
        } else {
            feedbackLabel.setText(user + " is wrong  -2");
            feedbackLabel.setForeground(new Color(230, 120, 120));
        }
    }

    private void onReveal(int correctIdx) {
        // Late reveal after steal round expires with no correct answer
        if (correctIdx >= 0 && correctIdx < 4) {
            optionButtons[correctIdx].setBackground(new Color(35, 160, 95));
        }
        setQuestionEnabled(false);
        buzzButton.setEnabled(false);
        if (feedbackLabel.getText() == null || feedbackLabel.getText().trim().isEmpty()) {
            feedbackLabel.setText("Answer revealed");
            feedbackLabel.setForeground(UIConstants.TEXT_MUTED);
        }
    }

    private void startCountdown(int seconds) {
        stopCountdown();
        secondsLeft = seconds;
        timerLabel.setText(secondsLeft + "s");
        countdownTimer = new Timer(1000, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                secondsLeft--;
                if (secondsLeft <= 0) {
                    timerLabel.setText("0s");
                    stopCountdown();
                } else {
                    timerLabel.setText(secondsLeft + "s");
                }
            }
        });
        countdownTimer.start();
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =============== Server messages ===============

    @Override
    public void onMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { processMessage(message); }
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JOptionPane.showMessageDialog(MultiplayerQuizView.this,
                        "Disconnected from server.", "Connection Lost",
                        JOptionPane.WARNING_MESSAGE);
                listener.onHomeClicked();
            }
        });
    }

    private void processMessage(String msg) {
        int firstColon = msg.indexOf(':');
        String head = firstColon < 0 ? msg : msg.substring(0, firstColon);
        String rest = firstColon < 0 ? ""  : msg.substring(firstColon + 1);

        switch (head) {
            case "QUESTION": {
                // QUESTION:qIdx:totalQ:primaryUser:question:opt0|opt1|opt2|opt3:timeSec
                String[] p = rest.split(":", 6);
                if (p.length < 6) return;
                try {
                    int qIdx   = Integer.parseInt(p[0]);
                    int totalQ = Integer.parseInt(p[1]);
                    String primary = p[2];
                    String qText = p[3];
                    // opts + time are glued in p[4] and p[5] -- actually our split
                    // with limit=6 gives [qIdx, totalQ, primary, qText, opts, timeSec]
                    String opts  = p[4];
                    int timeSec  = Integer.parseInt(p[5]);
                    String[] options = opts.split("\\|", -1);
                    if (options.length < 4) return;

                    progressLabel.setText("Question " + (qIdx + 1) + " / " + totalQ);
                    showPhase1(primary, qText, options, timeSec);
                    // rebuild scoreboard highlights (primary border gets thicker)
                    // scoreboard message itself isn't in QUESTION, so we keep the
                    // last one we had.
                } catch (NumberFormatException ignored) {}
                break;
            }
            case "PHASE2": {
                // PHASE2:qIdx:timeSec
                String[] p = rest.split(":");
                if (p.length < 2) return;
                try { int t = Integer.parseInt(p[1]); showPhase2(t); }
                catch (NumberFormatException ignored) {}
                break;
            }
            case "BUZZED": {
                // BUZZED:user:timeSec
                String[] p = rest.split(":");
                if (p.length < 2) return;
                try { onBuzzedMessage(p[0], Integer.parseInt(p[1])); }
                catch (NumberFormatException ignored) {}
                break;
            }
            case "ANSWERED": {
                // ANSWERED:user:optIdx:correct(0|1):scoreboard:correctIdx
                String[] p = rest.split(":");
                if (p.length < 5) return;
                try {
                    String user = p[0];
                    int optIdx  = Integer.parseInt(p[1]);
                    boolean correct = "1".equals(p[2]);
                    String scoreboard = p[3];
                    int correctIdx  = Integer.parseInt(p[4]);
                    onAnswered(user, optIdx, correct, correctIdx);
                    rebuildScoresPanel(scoreboard);
                } catch (NumberFormatException ignored) {}
                break;
            }
            case "REVEAL": {
                // REVEAL:correctIdx  (sent when steal round ends without a correct answer)
                try { onReveal(Integer.parseInt(rest)); }
                catch (NumberFormatException ignored) {}
                break;
            }
            case "QUIZEND": {
                // QUIZEND:winner:scoreboard
                String[] p = rest.split(":", 2);
                if (p.length < 2) return;
                stopCountdown();
                rebuildScoresPanel(p[1]);
                showQuizEndDialog(p[0], p[1]);
                break;
            }
            case "PLAYER_JOINED": {
                String[] p = rest.split(":");
                if (p.length >= 3) {
                    int sbStart = p[0].length() + 1 + p[1].length() + 1;
                    String sb = sbStart < rest.length() ? rest.substring(sbStart) : "";
                    if (!sb.isEmpty()) rebuildScoresPanel(sb);
                    phaseLabel.setText(p[0] + " joined the quiz");
                }
                break;
            }
            case "PLAYERLEFT": {
                String[] p = rest.split(":");
                if (p.length >= 2) {
                    int sbStart = p[0].length() + 1;
                    String sb = sbStart < rest.length() ? rest.substring(sbStart) : "";
                    if (!sb.isEmpty()) rebuildScoresPanel(sb);
                    phaseLabel.setText(p[0] + " left the quiz");
                }
                break;
            }
            case "ERROR": {
                feedbackLabel.setText("Error: " + rest);
                feedbackLabel.setForeground(new Color(230, 120, 120));
                break;
            }
            case "BLOCKED": {
                // just ignore silently or show muted
                phaseLabel.setText(rest);
                break;
            }
            default:
                // Ignore card-game messages if somehow routed here
                break;
        }
    }

    private void showQuizEndDialog(String winner, String scoreboard) {
        JDialog d = new JDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                "Quiz Complete", true);
        d.setUndecorated(true);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.BG_PANEL);
        panel.setBorder(new EmptyBorder(24, 36, 24, 36));

        String winnerText = winner.equals("TIE") ? "It's a tie!"
                : (winner.equals(myUsername) ? "You won!" : winner + " won!");

        JLabel hdr = new JLabel(winnerText, SwingConstants.CENTER);
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 22));
        hdr.setForeground(UIConstants.ACCENT_CYAN);
        hdr.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Final standings:", SwingConstants.CENTER);
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (String entry : scoreboard.split("~")) {
            String[] parts = entry.split("\\|");
            if (parts.length < 3) continue;
            JLabel row = new JLabel(parts[0] + " — " + parts[1] + " pts");
            row.setFont(new Font("Segoe UI", Font.BOLD, 14));
            try { row.setForeground(Color.decode(parts[2])); }
            catch (Exception ex) { row.setForeground(Color.WHITE); }
            row.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(row);
        }

        JButton ok = new JButton("Back to Home");
        ok.setFont(UIConstants.FONT_BUTTON);
        ok.setBackground(UIConstants.ACCENT_BLUE);
        ok.setForeground(Color.WHITE);
        ok.setOpaque(true);
        ok.setFocusPainted(false);
        ok.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                d.dispose();
                client.quit(roomId, myUsername);
                client.disconnect();
                listener.onHomeClicked();
            }
        });

        panel.add(hdr);
        panel.add(Box.createVerticalStrut(10));
        panel.add(sub);
        panel.add(Box.createVerticalStrut(8));
        panel.add(list);
        panel.add(Box.createVerticalStrut(16));
        panel.add(ok);

        d.setContentPane(panel);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }
}