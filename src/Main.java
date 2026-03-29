import controller.AuthController;
import controller.GameController;
import model.Card;
import model.GameBoard;
import model.GameState;
import network.GameClient;
import util.UIConstants;
import view.CategorySelectionView;
import view.GameBoardView;
import view.HomeView;
import view.LoginView;
import view.MultiplayerBoardView;
import view.MultiplayerView;
import view.ResultDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Memory Game - Main Entry Point
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

    private static CardLayout rootLayout;
    private static JPanel     rootPanel;
    private static GameController activeController;

    // ----------------------------------------------------------
    //  Entry Point
    // ----------------------------------------------------------

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("Panel.background",             UIConstants.BG_DARK);
        UIManager.put("OptionPane.background",        UIConstants.BG_PANEL);
        UIManager.put("OptionPane.messageForeground", UIConstants.TEXT_PRIMARY);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
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

        // Add placeholders for every screen
        addPlaceholder(SCREEN_LOGIN);
        addPlaceholder(SCREEN_HOME);
        addPlaceholder(SCREEN_CATEGORY);
        addPlaceholder(SCREEN_GAME);
        addPlaceholder(SCREEN_MULTI);
        addPlaceholder(SCREEN_MPGAME);

        frame.setContentPane(rootPanel);
        frame.setVisible(true);

        // Load the real login screen
        replaceScreen(SCREEN_LOGIN, buildLoginScreen());
        showScreen(SCREEN_LOGIN);
    }

    private static void addPlaceholder(String name) {
        JPanel p = new JPanel();
        p.setBackground(UIConstants.BG_DARK);
        p.setName(name);
        rootPanel.add(p, name);
    }

    // ----------------------------------------------------------
    //  Screen Builders
    // ----------------------------------------------------------

    private static LoginView buildLoginScreen() {
        return new LoginView(authController, new LoginView.LoginListener() {
            @Override
            public void onLoginSuccess(String username) {
                currentUsername = username;
                replaceScreen(SCREEN_HOME, buildHomeScreen());
                showScreen(SCREEN_HOME);
            }
        });
    }

    private static HomeView buildHomeScreen() {
        return new HomeView(currentUsername, new HomeView.HomeListener() {
            @Override
            public void onSinglePlayer() {
                replaceScreen(SCREEN_CATEGORY, buildCategoryScreen());
                showScreen(SCREEN_CATEGORY);
            }
            @Override
            public void onMultiplayer() {
                replaceScreen(SCREEN_MULTI, buildMultiplayerScreen());
                showScreen(SCREEN_MULTI);
            }
            @Override
            public void onLogout() {
                authController.logout();
                currentUsername = null;
                replaceScreen(SCREEN_LOGIN, buildLoginScreen());
                showScreen(SCREEN_LOGIN);
            }
        });
    }

    private static CategorySelectionView buildCategoryScreen() {
        return new CategorySelectionView(new CategorySelectionView.SelectionListener() {
            @Override
            public void onSelectionConfirmed(Card.Category category, int level) {
                launchSinglePlayerGame(level, category);
            }
            @Override
            public void onBack() {
                goHome();
            }
        });
    }

    private static MultiplayerView buildMultiplayerScreen() {
        return new MultiplayerView(currentUsername, new MultiplayerView.MultiplayerListener() {
            @Override
            public void onGameStart(String roomId, String username, GameClient client,
                                    int rows, int cols, String[] boardValues,
                                    Card.Category category) {
                launchMultiplayerGame(roomId, username, client, rows, cols, boardValues, category);
            }
            @Override
            public void onBack() {
                goHome();
            }
        });
    }

    // ----------------------------------------------------------
    //  Game Launch
    // ----------------------------------------------------------

    private static void launchSinglePlayerGame(final int level, final Card.Category category) {
        if (activeController != null) {
            activeController.stopClock();
        }

        final GameBoard board = new GameBoard(level, category);
        final GameState state = new GameState(currentUsername, level, category);

        final GameBoardView boardView = new GameBoardView(board, state,
                new GameBoardView.BoardListener() {
                    @Override
                    public void onCardClicked(int row, int col) {
                        if (activeController != null) {
                            activeController.onCardClicked(row, col);
                        }
                    }
                    @Override
                    public void onHomeClicked() {
                        if (activeController != null) {
                            activeController.stopClock();
                        }
                        goHome();
                    }
                });

        final GameController controller = new GameController(state, board, boardView);
        activeController = controller;

        controller.setLevelCompleteListener(new GameController.LevelCompleteListener() {
            @Override
            public void onLevelComplete(GameState finalState) {
                finalState.freezeTime();
                authController.updateCurrentUser(finalState.getScore(), 1);

                boolean hasNext = (level < 10);
                ResultDialog dialog = new ResultDialog(frame, finalState, hasNext,
                        new ResultDialog.ResultListener() {
                            @Override
                            public void onChoice(ResultDialog.Choice choice) {
                                if (choice == ResultDialog.Choice.NEXT_LEVEL) {
                                    launchSinglePlayerGame(level + 1, category);
                                } else if (choice == ResultDialog.Choice.REPLAY) {
                                    launchSinglePlayerGame(level, category);
                                } else {
                                    goHome();
                                }
                            }
                        });
                dialog.setVisible(true);
            }
        });

        // Switch to game screen FIRST, then start preview
        replaceScreen(SCREEN_GAME, boardView);
        showScreen(SCREEN_GAME);

        int previewMs = GameBoard.getPreviewTime(level);
        controller.startLevelPreview(previewMs);
    }

    private static void launchMultiplayerGame(String roomId, String username,
                                               GameClient client, int rows, int cols,
                                               String[] boardValues, Card.Category category) {
        MultiplayerBoardView mpView = new MultiplayerBoardView(
                roomId, username, client, rows, cols, boardValues, category,
                new MultiplayerBoardView.MPBoardListener() {
                    @Override
                    public void onHomeClicked() {
                        goHome();
                    }
                });

        replaceScreen(SCREEN_MPGAME, mpView);
        showScreen(SCREEN_MPGAME);
    }

    // ----------------------------------------------------------
    //  Navigation Helpers
    // ----------------------------------------------------------

    private static void goHome() {
        replaceScreen(SCREEN_HOME, buildHomeScreen());
        showScreen(SCREEN_HOME);
    }

    /**
     * Show the named screen using CardLayout.
     * Forces a full repaint to prevent overlay artifacts.
     */
    private static void showScreen(String name) {
        rootLayout.show(rootPanel, name);
        rootPanel.revalidate();
        rootPanel.repaint();
        frame.revalidate();
        frame.repaint();
    }

    /**
     * Safely replace a screen panel by name.
     * Removes old panel, adds new one, then revalidates.
     */
    private static void replaceScreen(String name, JPanel newPanel) {
        // Remove old panel with this name
        Component toRemove = null;
        for (Component c : rootPanel.getComponents()) {
            if (name.equals(c.getName())) {
                toRemove = c;
                break;
            }
        }
        if (toRemove != null) {
            rootPanel.remove(toRemove);
        }

        // Add new panel
        newPanel.setName(name);
        rootPanel.add(newPanel, name);
        rootPanel.revalidate();
        rootPanel.repaint();
    }
}