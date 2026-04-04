import controller.AuthController;
import controller.GameController;
import model.Card;
import model.GameBoard;
import model.GameState;
import network.GameClient;
import quiz.Question;
import quiz.QuizBank;
import quiz.QuizController;
import util.UIConstants;
import view.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Memory Game - Main Entry Point
 * Now includes: Card Memory, Quiz Mode, Brain Training, Multiplayer
 */
public class Main {

    private static JFrame         frame;
    private static AuthController authController;
    private static String         currentUsername;

    private static final String SCREEN_LOGIN    = "LOGIN";
    private static final String SCREEN_HOME     = "HOME";
    private static final String SCREEN_CATEGORY = "CATEGORY";
    private static final String SCREEN_GAME     = "GAME";
    private static final String SCREEN_MULTI    = "MULTIPLAYER";
    private static final String SCREEN_MPGAME   = "MPGAME";
    private static final String SCREEN_QUIZ_SEL = "QUIZ_SEL";
    private static final String SCREEN_QUIZ     = "QUIZ";
    private static final String SCREEN_QUIZ_RES = "QUIZ_RES";
    private static final String SCREEN_BRAIN    = "BRAIN";
    private static final String SCREEN_PATH     = "PATH";
    private static final String SCREEN_SPOT     = "SPOT";
    private static final String SCREEN_JIGSAW   = "JIGSAW";

    private static CardLayout rootLayout;
    private static JPanel     rootPanel;
    private static GameController activeController;

    // ---- Entry Point ----
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
        UIManager.put("Panel.background",             UIConstants.BG_DARK);
        UIManager.put("OptionPane.background",        UIConstants.BG_PANEL);
        UIManager.put("OptionPane.messageForeground", UIConstants.TEXT_PRIMARY);

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { createAndShowGUI(); }
        });
    }

    private static void createAndShowGUI() {
        authController = new AuthController();
        frame = new JFrame("Memory Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT);
        frame.setMinimumSize(new Dimension(700, 500));
        frame.setLocationRelativeTo(null);

        rootLayout = new CardLayout();
        rootPanel  = new JPanel(rootLayout);
        rootPanel.setBackground(UIConstants.BG_DARK);

        // Add all screen placeholders
        String[] screens = {
            SCREEN_LOGIN, SCREEN_HOME, SCREEN_CATEGORY, SCREEN_GAME,
            SCREEN_MULTI, SCREEN_MPGAME, SCREEN_QUIZ_SEL, SCREEN_QUIZ,
            SCREEN_QUIZ_RES, SCREEN_BRAIN, SCREEN_PATH, SCREEN_SPOT, SCREEN_JIGSAW
        };
        for (String s : screens) addPlaceholder(s);

        frame.setContentPane(rootPanel);
        frame.setVisible(true);

        replaceScreen(SCREEN_LOGIN, buildLoginScreen());
        showScreen(SCREEN_LOGIN);
    }

    private static void addPlaceholder(String name) {
        JPanel p = new JPanel();
        p.setBackground(UIConstants.BG_DARK);
        p.setName(name);
        rootPanel.add(p, name);
    }

    // ---- Screen Builders ----

    private static LoginView buildLoginScreen() {
        return new LoginView(authController, new LoginView.LoginListener() {
            @Override public void onLoginSuccess(String username) {
                currentUsername = username;
                replaceScreen(SCREEN_HOME, buildHomeScreen());
                showScreen(SCREEN_HOME);
            }
        });
    }

    private static HomeView buildHomeScreen() {
        return new HomeView(currentUsername, new HomeView.HomeListener() {
            @Override public void onSinglePlayer() {
                replaceScreen(SCREEN_CATEGORY, buildCategoryScreen());
                showScreen(SCREEN_CATEGORY);
            }
            @Override public void onMultiplayer() {
                replaceScreen(SCREEN_MULTI, buildMultiplayerScreen());
                showScreen(SCREEN_MULTI);
            }
            @Override public void onQuizMode() {
                replaceScreen(SCREEN_QUIZ_SEL, buildQuizSelectionScreen());
                showScreen(SCREEN_QUIZ_SEL);
            }
            @Override public void onBrainTraining() {
                replaceScreen(SCREEN_BRAIN, buildBrainMenuScreen());
                showScreen(SCREEN_BRAIN);
            }
            @Override public void onLogout() {
                authController.logout();
                currentUsername = null;
                replaceScreen(SCREEN_LOGIN, buildLoginScreen());
                showScreen(SCREEN_LOGIN);
            }
        });
    }

    private static CategorySelectionView buildCategoryScreen() {
        return new CategorySelectionView(new CategorySelectionView.SelectionListener() {
            @Override public void onSelectionConfirmed(Card.Category category, int level) {
                launchSinglePlayerGame(level, category);
            }
            @Override public void onBack() { goHome(); }
        });
    }

    private static MultiplayerView buildMultiplayerScreen() {
        return new MultiplayerView(currentUsername, new MultiplayerView.MultiplayerListener() {
            @Override
            public void onGameStart(String roomId, String username, GameClient client,
                                    int rows, int cols, String[] boardValues,
                                    Card.Category category, String myColor,
                                    String scoreboard, String firstTurnPlayer) {
                launchMultiplayerGame(roomId, username, client, rows, cols,
                        boardValues, category, myColor, scoreboard, firstTurnPlayer);
            }
            @Override public void onBack() { goHome(); }
        });
    }

    // ---- Quiz Screens ----

    private static QuizSelectionView buildQuizSelectionScreen() {
        return new QuizSelectionView(new QuizSelectionView.QuizSelectionListener() {
            @Override
            public void onStartQuiz(Question.Subject subject, Question.Difficulty difficulty) {
                launchQuiz(subject, difficulty);
            }
            @Override public void onBack() { goHome(); }
        });
    }

    private static void launchQuiz(Question.Subject subject, Question.Difficulty difficulty) {
        List<Question> questions = QuizBank.getRandom(subject, 10);
        if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No questions found for this subject and difficulty.",
                    "No Questions", JOptionPane.WARNING_MESSAGE);
            return;
        }
        QuizController quizCtrl = new QuizController(questions);
        QuizGameView quizView = new QuizGameView(quizCtrl, new QuizGameView.QuizGameListener() {
            @Override public void onQuizComplete(QuizController controller) {
                authController.updateCurrentUser(controller.getScore(), 1);
                QuizResultView result = new QuizResultView(controller,
                        new QuizResultView.ResultListener() {
                            @Override public void onPlayAgain() {
                                launchQuiz(subject, difficulty);
                            }
                            @Override public void onHome() { goHome(); }
                        });
                replaceScreen(SCREEN_QUIZ_RES, result);
                showScreen(SCREEN_QUIZ_RES);
            }
            @Override public void onHome() { goHome(); }
        });
        replaceScreen(SCREEN_QUIZ, quizView);
        showScreen(SCREEN_QUIZ);
    }

    // ---- Brain Training Screens ----

    private static BrainTrainingMenuView buildBrainMenuScreen() {
        return new BrainTrainingMenuView(currentUsername,
                new BrainTrainingMenuView.BrainMenuListener() {
            @Override public void onPathMemory() {
                PathMemoryView view = new PathMemoryView(1,
                        new PathMemoryView.PathMemoryListener() {
                    @Override public void onBack() { goHome(); }
                    @Override public void onLevelComplete(int level, int score) {
                        authController.updateCurrentUser(score, 1);
                    }
                });
                replaceScreen(SCREEN_PATH, view);
                showScreen(SCREEN_PATH);
            }
            @Override public void onSpotDifference() {
                SpotDifferenceView view = new SpotDifferenceView(1,
                        new SpotDifferenceView.SpotListener() {
                    @Override public void onBack() { goHome(); }
                });
                replaceScreen(SCREEN_SPOT, view);
                showScreen(SCREEN_SPOT);
            }
            @Override public void onJigsaw() {
                JigsawView view = new JigsawView(1,
                        new JigsawView.JigsawListener() {
                    @Override public void onBack() { goHome(); }
                });
                replaceScreen(SCREEN_JIGSAW, view);
                showScreen(SCREEN_JIGSAW);
            }
            @Override public void onBack() { goHome(); }
        });
    }

    // ---- Game Launch ----

    private static void launchSinglePlayerGame(final int level, final Card.Category category) {
        if (activeController != null) activeController.stopClock();

        GameBoard board  = new GameBoard(level, category);
        GameState state  = new GameState(currentUsername, level, category);

        GameBoardView boardView = new GameBoardView(board, state,
                new GameBoardView.BoardListener() {
                    @Override public void onCardClicked(int row, int col) {
                        if (activeController != null) activeController.onCardClicked(row, col);
                    }
                    @Override public void onHomeClicked() {
                        if (activeController != null) activeController.stopClock();
                        goHome();
                    }
                });

        GameController controller = new GameController(state, board, boardView);
        activeController = controller;

        controller.setLevelCompleteListener(new GameController.LevelCompleteListener() {
            @Override public void onLevelComplete(GameState finalState) {
                finalState.freezeTime();
                authController.updateCurrentUser(finalState.getScore(), 1);
                boolean hasNext = level < 10;
                ResultDialog dialog = new ResultDialog(frame, finalState, hasNext,
                        new ResultDialog.ResultListener() {
                            @Override public void onChoice(ResultDialog.Choice choice) {
                                if      (choice == ResultDialog.Choice.NEXT_LEVEL)
                                    launchSinglePlayerGame(level + 1, category);
                                else if (choice == ResultDialog.Choice.REPLAY)
                                    launchSinglePlayerGame(level, category);
                                else goHome();
                            }
                        });
                dialog.setVisible(true);
            }
        });

        replaceScreen(SCREEN_GAME, boardView);
        showScreen(SCREEN_GAME);
        controller.startLevelPreview(GameBoard.getPreviewTime(level));
    }

    private static void launchMultiplayerGame(String roomId, String username,
                                               GameClient client, int rows, int cols,
                                               String[] boardValues, Card.Category category,
                                               String myColor, String scoreboard,
                                               String firstTurnPlayer) {
        MultiplayerBoardView mpView = new MultiplayerBoardView(
                roomId, username, client, rows, cols, boardValues, category,
                myColor, scoreboard, firstTurnPlayer,
                new MultiplayerBoardView.MPBoardListener() {
                    @Override public void onHomeClicked() { goHome(); }
                });
        replaceScreen(SCREEN_MPGAME, mpView);
        showScreen(SCREEN_MPGAME);
    }

    // ---- Navigation Helpers ----

    private static void goHome() {
        replaceScreen(SCREEN_HOME, buildHomeScreen());
        showScreen(SCREEN_HOME);
    }

    private static void showScreen(String name) {
        rootLayout.show(rootPanel, name);
        rootPanel.revalidate();
        rootPanel.repaint();
        frame.revalidate();
        frame.repaint();
    }

    private static void replaceScreen(String name, JPanel newPanel) {
        Component toRemove = null;
        for (Component c : rootPanel.getComponents()) {
            if (name.equals(c.getName())) { toRemove = c; break; }
        }
        if (toRemove != null) rootPanel.remove(toRemove);
        newPanel.setName(name);
        rootPanel.add(newPanel, name);
        rootPanel.revalidate();
        rootPanel.repaint();
    }
}