package controller;

import model.Card;
import model.GameBoard;
import model.GameState;
import view.GameBoardView;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * GameController wires the game model to the board view.
 * Implements the core flip/match logic and level progression.
 */
public class GameController {

    private GameBoard board;
    private GameState state;
    private GameBoardView boardView;

    // Timer to auto-flip mismatched cards back
    private Timer flipBackTimer;
    private int firstRow, firstCol, secondRow, secondCol;

    // Timer to count elapsed seconds for the HUD
    private Timer clockTimer;

    // Callback interface for level completion events
    public interface LevelCompleteListener {
        void onLevelComplete(GameState finalState);
    }

    private LevelCompleteListener levelCompleteListener;

    public GameController(GameState state, GameBoard board, GameBoardView boardView) {
        this.state     = state;
        this.board     = board;
        this.boardView = boardView;
        setupClockTimer();
    }

    public void setLevelCompleteListener(LevelCompleteListener listener) {
        this.levelCompleteListener = listener;
    }

    // --- Timer Setup ---

    private void setupClockTimer() {
        clockTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boardView.updateHUD(
                    state.getPlayerName(),
                    state.getScore(),
                    state.getLevel(),
                    (int) state.getElapsedSeconds()
                );
            }
        });
    }

    public void startClock() { clockTimer.start(); }
    public void stopClock()  { clockTimer.stop();  }

    // --- Card Click Handler ---

    /**
     * Called by the view when a card is clicked.
     */
    public void onCardClicked(int row, int col) {
        Card card = board.getCard(row, col);

        // Ignore if already matched, already flipped, or mid-animation
        if (card.isMatched() || card.isFlipped()) return;
        if (flipBackTimer != null && flipBackTimer.isRunning()) return;

        card.flip();
        boardView.refreshCard(row, col);

        if (!state.isWaitingForSecond()) {
            // First card flipped this turn
            state.setFirstFlipped(card);
            firstRow = row;
            firstCol = col;
        } else {
            // Second card flipped - evaluate match
            secondRow = row;
            secondCol = col;
            state.incrementAttempts();
            evaluateFlip();
        }
    }

    private void evaluateFlip() {
        Card first  = state.getFirstFlipped();
        Card second = board.getCard(secondRow, secondCol);

        if (first.matches(second)) {
            // Matched!
            board.registerMatch(firstRow, firstCol, secondRow, secondCol);
            state.registerMatch();
            state.clearFirstFlipped();
            boardView.refreshCard(firstRow, firstCol);
            boardView.refreshCard(secondRow, secondCol);
            boardView.updateHUD(state.getPlayerName(), state.getScore(),
                    state.getLevel(), (int) state.getElapsedSeconds());

            // Check level completion
            if (board.isComplete()) {
                stopClock();
                state.freezeTime();
                state.setLevelComplete(true);
                if (levelCompleteListener != null) {
                    levelCompleteListener.onLevelComplete(state);
                }
            }
        } else {
            // Mismatch - flip back after a short delay
            state.registerMiss();
            boardView.updateHUD(state.getPlayerName(), state.getScore(),
                    state.getLevel(), (int) state.getElapsedSeconds());

            final int fr = firstRow, fc = firstCol, sr = secondRow, sc = secondCol;
            state.clearFirstFlipped();

            flipBackTimer = new Timer(900, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    board.getCard(fr, fc).hide();
                    board.getCard(sr, sc).hide();
                    boardView.refreshCard(fr, fc);
                    boardView.refreshCard(sr, sc);
                    flipBackTimer.stop();
                }
            });
            flipBackTimer.setRepeats(false);
            flipBackTimer.start();
        }
    }

    // --- Preview Logic ---

    /**
     * Show all cards for preview duration, then hide them and start the clock.
     */
    public void startLevelPreview(int previewMs) {
        board.revealAll();
        boardView.refreshAllCards();

        Timer hideTimer = new Timer(previewMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                board.coverAll();
                boardView.refreshAllCards();
                startClock();
            }
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    // --- Accessors ---
    public GameState getState() { return state; }
    public GameBoard getBoard() { return board; }
}