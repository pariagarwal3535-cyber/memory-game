package util;

import javax.swing.*;
import java.awt.Toolkit;

/**
 * Provides simple audio feedback using system beep + visual flash.
 * Requires no external audio files.
 */
public class SoundUtil {

    public static void playMatch() {
        // System bell as minimal audio feedback
        Toolkit.getDefaultToolkit().beep();
    }

    public static void playMiss() {
        // Brief pause then beep to feel different from match
        Timer t = new Timer(100, e -> {
            Toolkit.getDefaultToolkit().beep();
            ((Timer)e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }

    public static void playLevelComplete() {
        for (int i = 0; i < 3; i++) {
            final int delay = i * 150;
            Timer t = new Timer(delay, e -> {
                Toolkit.getDefaultToolkit().beep();
                ((Timer)e.getSource()).stop();
            });
            t.setRepeats(false);
            t.start();
        }
    }

    private SoundUtil() {}
}