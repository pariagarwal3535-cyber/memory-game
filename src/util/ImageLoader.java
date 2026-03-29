package util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches card images from the resources/images/ folder.
 * If an image file is missing, draws a colored placeholder automatically.
 *
 * Folder structure expected:
 *   resources/images/animals/   -> cat.png, dog.png, etc.
 *   resources/images/fruits/    -> apple.png, banana.png, etc.
 *   resources/images/shapes/    -> circle.png, star.png, etc.
 *   resources/images/emojis/    -> happy.png, sad.png, etc.
 */
public class ImageLoader {

    // Cache: "category/filename" -> scaled ImageIcon
    private static final Map<String, ImageIcon> cache = new HashMap<String, ImageIcon>();

    // Base path for all images (relative to project root)
    private static final String BASE_PATH = "resources" + File.separator + "images";

    /**
     * Load an image for the given category and filename.
     * Returns a scaled ImageIcon fitted to the given size.
     * Falls back to a colored placeholder if file not found.
     */
    public static ImageIcon loadImage(String category, String filename, int size) {
        String key = category + "/" + filename + "_" + size;

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        // Build file path: resources/images/animals/cat.png
        String path = BASE_PATH + File.separator
                + category.toLowerCase() + File.separator + filename;

        File file = new File(path);
        ImageIcon icon;

        if (file.exists()) {
            // Load real image and scale it
            ImageIcon raw = new ImageIcon(path);
            Image scaled = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaled);
        } else {
            // Create a colored placeholder with the filename as label
            icon = createPlaceholder(filename, size);
        }

        cache.put(key, icon);
        return icon;
    }

    /**
     * Creates a colored placeholder image when the real file is missing.
     * Each unique name gets a consistent color based on its hash.
     */
    private static ImageIcon createPlaceholder(String name, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Pick a color based on name hash so same name = same color
        float hue = (Math.abs(name.hashCode()) % 360) / 360.0f;
        Color bg = Color.getHSBColor(hue, 0.6f, 0.85f);

        // Draw rounded rectangle background
        g2.setColor(bg);
        g2.fillRoundRect(2, 2, size - 4, size - 4, 14, 14);

        // Draw border
        g2.setColor(bg.darker());
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(2, 2, size - 4, size - 4, 14, 14);

        // Draw label text (first 6 chars, or full name if short)
        String label = name.replace(".png", "").replace(".jpg", "");
        if (label.length() > 6) label = label.substring(0, 6);

        g2.setColor(new Color(30, 30, 30));
        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(10, size / 6)));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (size - fm.stringWidth(label)) / 2;
        int ty = (size + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(label, tx, ty);

        g2.dispose();
        return new ImageIcon(img);
    }

    /**
     * Returns all image filenames found in a category folder.
     * Falls back to default names if folder is empty or missing.
     */
    public static String[] getImageNames(String category) {
        File folder = new File(BASE_PATH + File.separator + category.toLowerCase());

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles(f ->
                f.getName().toLowerCase().endsWith(".png") ||
                f.getName().toLowerCase().endsWith(".jpg") ||
                f.getName().toLowerCase().endsWith(".jpeg")
            );

            if (files != null && files.length >= 2) {
                String[] names = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    names[i] = files[i].getName();
                }
                return names;
            }
        }

        // Fallback names if folder is empty/missing
        return getFallbackNames(category);
    }

    /**
     * Fallback image names used when the folder has no images.
     * These will render as colored placeholders.
     */
    private static String[] getFallbackNames(String category) {
        switch (category.toLowerCase()) {
            case "animals":
                return new String[]{
                    "cat.png","dog.png","lion.png","tiger.png","elephant.png",
                    "giraffe.png","zebra.png","monkey.png","bear.png","fox.png",
                    "rabbit.png","panda.png","penguin.png","owl.png","parrot.png",
                    "frog.png","dolphin.png","shark.png","eagle.png","horse.png",
                    "cow.png","pig.png"
                };
            case "fruits":
                return new String[]{
                    "apple.png","banana.png","orange.png","grapes.png","strawberry.png",
                    "watermelon.png","mango.png","pineapple.png","cherry.png","pear.png",
                    "kiwi.png","lemon.png","peach.png","plum.png","blueberry.png",
                    "coconut.png","papaya.png","guava.png","fig.png","apricot.png",
                    "lychee.png","melon.png"
                };
            case "shapes":
                return new String[]{
                    "circle.png","square.png","triangle.png","star.png","heart.png",
                    "diamond.png","pentagon.png","hexagon.png","octagon.png","oval.png",
                    "arrow.png","cross.png","crescent.png","parallelogram.png","trapezoid.png",
                    "rhombus.png","spiral.png","wave.png","zigzag.png","ring.png",
                    "cube.png","cylinder.png"
                };
            case "emojis":
            default:
                return new String[]{
                    "happy.png","sad.png","angry.png","surprised.png","love.png",
                    "cool.png","wink.png","laugh.png","cry.png","think.png",
                    "sleepy.png","nervous.png","sick.png","silly.png","grin.png",
                    "worried.png","confused.png","excited.png","bored.png","proud.png",
                    "shy.png","scared.png"
                };
        }
    }

    /** Clear image cache (call when changing categories) */
    public static void clearCache() {
        cache.clear();
    }
}