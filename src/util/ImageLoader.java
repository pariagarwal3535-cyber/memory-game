package util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches card images from the resources/images/ folder.
 *
 * If real image files exist:  loads and scales them.
 * If no images found:         renders a built-in emoji/symbol for the card.
 *
 * Built-in emoji mapping is used so the game looks great
 * even without downloading any image dataset.
 *
 * Folder structure (optional):
 *   resources/images/animals/cat.png, dog.png ...
 *   resources/images/fruits/apple.png, banana.png ...
 *   resources/images/shapes/circle.png, star.png ...
 *   resources/images/emojis/happy.png, sad.png ...
 */
public class ImageLoader {

    private static final Map<String, ImageIcon> cache = new HashMap<String, ImageIcon>();
    private static final String BASE_PATH = "resources" + File.separator + "images";

    // ---- Built-in emoji/symbol maps ----
    // These are used when no image file is found, giving a great look by default.

    private static final Map<String, String> EMOJI_MAP = new HashMap<String, String>();
    private static final Map<String, String> ANIMAL_MAP = new HashMap<String, String>();
    private static final Map<String, String> FRUIT_MAP = new HashMap<String, String>();
    private static final Map<String, String> SHAPE_MAP = new HashMap<String, String>();

    static {
        // Emojis
        EMOJI_MAP.put("happy.png",     "\uD83D\uDE00");
        EMOJI_MAP.put("sad.png",       "\uD83D\uDE22");
        EMOJI_MAP.put("angry.png",     "\uD83D\uDE21");
        EMOJI_MAP.put("surprised.png", "\uD83D\uDE32");
        EMOJI_MAP.put("love.png",      "\uD83D\uDE0D");
        EMOJI_MAP.put("cool.png",      "\uD83D\uDE0E");
        EMOJI_MAP.put("wink.png",      "\uD83D\uDE09");
        EMOJI_MAP.put("laugh.png",     "\uD83D\uDE02");
        EMOJI_MAP.put("cry.png",       "\uD83D\uDE2D");
        EMOJI_MAP.put("think.png",     "\uD83E\uDD14");
        EMOJI_MAP.put("sleepy.png",    "\uD83D\uDE34");
        EMOJI_MAP.put("nervous.png",   "\uD83D\uDE1F");
        EMOJI_MAP.put("sick.png",      "\uD83E\uDD22");
        EMOJI_MAP.put("silly.png",     "\uD83D\uDE1C");
        EMOJI_MAP.put("grin.png",      "\uD83D\uDE01");
        EMOJI_MAP.put("worried.png",   "\uD83D\uDE1F");
        EMOJI_MAP.put("confused.png",  "\uD83D\uDE15");
        EMOJI_MAP.put("excited.png",   "\uD83E\uDD29");
        EMOJI_MAP.put("bored.png",     "\uD83D\uDE10");
        EMOJI_MAP.put("proud.png",     "\uD83E\uDD73");
        EMOJI_MAP.put("shy.png",       "\uD83D\uDE0A");
        EMOJI_MAP.put("scared.png",    "\uD83D\uDE28");

        // Animals
        ANIMAL_MAP.put("cat.png",      "\uD83D\uDC31");
        ANIMAL_MAP.put("dog.png",      "\uD83D\uDC36");
        ANIMAL_MAP.put("lion.png",     "\uD83E\uDD81");
        ANIMAL_MAP.put("tiger.png",    "\uD83D\uDC2F");
        ANIMAL_MAP.put("elephant.png", "\uD83D\uDC18");
        ANIMAL_MAP.put("giraffe.png",  "\uD83E\uDD92");
        ANIMAL_MAP.put("zebra.png",    "\uD83E\uDD93");
        ANIMAL_MAP.put("monkey.png",   "\uD83D\uDC12");
        ANIMAL_MAP.put("bear.png",     "\uD83D\uDC3B");
        ANIMAL_MAP.put("fox.png",      "\uD83E\uDD8A");
        ANIMAL_MAP.put("rabbit.png",   "\uD83D\uDC30");
        ANIMAL_MAP.put("panda.png",    "\uD83D\uDC3C");
        ANIMAL_MAP.put("penguin.png",  "\uD83D\uDC27");
        ANIMAL_MAP.put("owl.png",      "\uD83E\uDD89");
        ANIMAL_MAP.put("parrot.png",   "\uD83E\uDD9C");
        ANIMAL_MAP.put("frog.png",     "\uD83D\uDC38");
        ANIMAL_MAP.put("dolphin.png",  "\uD83D\uDC2C");
        ANIMAL_MAP.put("shark.png",    "\uD83E\uDD88");
        ANIMAL_MAP.put("eagle.png",    "\uD83E\uDD85");
        ANIMAL_MAP.put("horse.png",    "\uD83D\uDC34");
        ANIMAL_MAP.put("cow.png",      "\uD83D\uDC04");
        ANIMAL_MAP.put("pig.png",      "\uD83D\uDC37");

        // Fruits
        FRUIT_MAP.put("apple.png",       "\uD83C\uDF4E");
        FRUIT_MAP.put("banana.png",      "\uD83C\uDF4C");
        FRUIT_MAP.put("orange.png",      "\uD83C\uDF4A");
        FRUIT_MAP.put("grapes.png",      "\uD83C\uDF47");
        FRUIT_MAP.put("strawberry.png",  "\uD83C\uDF53");
        FRUIT_MAP.put("watermelon.png",  "\uD83C\uDF49");
        FRUIT_MAP.put("mango.png",       "\uD83E\uDD6D");
        FRUIT_MAP.put("pineapple.png",   "\uD83C\uDF4D");
        FRUIT_MAP.put("cherry.png",      "\uD83C\uDF52");
        FRUIT_MAP.put("pear.png",        "\uD83C\uDF50");
        FRUIT_MAP.put("kiwi.png",        "\uD83E\uDD5D");
        FRUIT_MAP.put("lemon.png",       "\uD83C\uDF4B");
        FRUIT_MAP.put("peach.png",       "\uD83C\uDF51");
        FRUIT_MAP.put("plum.png",        "\uD83C\uDF51");
        FRUIT_MAP.put("blueberry.png",   "\uD83E\uDED0");
        FRUIT_MAP.put("coconut.png",     "\uD83E\uDD65");
        FRUIT_MAP.put("papaya.png",      "\uD83E\uDD6D");
        FRUIT_MAP.put("guava.png",       "\uD83C\uDF4F");
        FRUIT_MAP.put("fig.png",         "\uD83C\uDF51");
        FRUIT_MAP.put("apricot.png",     "\uD83C\uDF51");
        FRUIT_MAP.put("lychee.png",      "\uD83C\uDF53");
        FRUIT_MAP.put("melon.png",       "\uD83C\uDF48");

        // Shapes
        SHAPE_MAP.put("circle.png",        "\u25CF");
        SHAPE_MAP.put("square.png",        "\u25A0");
        SHAPE_MAP.put("triangle.png",      "\u25B2");
        SHAPE_MAP.put("star.png",          "\u2605");
        SHAPE_MAP.put("heart.png",         "\u2665");
        SHAPE_MAP.put("diamond.png",       "\u25C6");
        SHAPE_MAP.put("pentagon.png",      "\u2B1F");
        SHAPE_MAP.put("hexagon.png",       "\u2B22");
        SHAPE_MAP.put("octagon.png",       "\u2B23");
        SHAPE_MAP.put("oval.png",          "\u2B2D");
        SHAPE_MAP.put("arrow.png",         "\u2794");
        SHAPE_MAP.put("cross.png",         "\u271A");
        SHAPE_MAP.put("crescent.png",      "\u263D");
        SHAPE_MAP.put("parallelogram.png", "\u25B1");
        SHAPE_MAP.put("trapezoid.png",     "\u25B3");
        SHAPE_MAP.put("rhombus.png",       "\u25C7");
        SHAPE_MAP.put("spiral.png",        "\u29B5");
        SHAPE_MAP.put("wave.png",          "\u223C");
        SHAPE_MAP.put("zigzag.png",        "\u2A4D");
        SHAPE_MAP.put("ring.png",          "\u25CB");
        SHAPE_MAP.put("cube.png",          "\u25A1");
        SHAPE_MAP.put("cylinder.png",      "\u29C4");
    }

    /**
     * Load an image icon for the given category and filename.
     * Priority:
     *   1. Real image file from resources/images/<category>/<filename>
     *   2. Built-in emoji/symbol rendered as ImageIcon
     */
    public static ImageIcon loadImage(String category, String filename, int size) {
        String key = category + "/" + filename + "_" + size;
        if (cache.containsKey(key)) return cache.get(key);

        // Try real image file first
        String path = BASE_PATH + File.separator
                + category.toLowerCase() + File.separator + filename;
        File file = new File(path);
        ImageIcon icon;

        if (file.exists()) {
            ImageIcon raw    = new ImageIcon(path);
            Image    scaled  = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaled);
        } else {
            // Use built-in emoji/symbol
            String symbol = getBuiltinSymbol(category, filename);
            icon = renderSymbolAsIcon(symbol, size, category);
        }

        cache.put(key, icon);
        return icon;
    }

    /**
     * Get the built-in emoji or symbol for a given filename.
     */
    private static String getBuiltinSymbol(String category, String filename) {
        String sym = null;
        switch (category.toLowerCase()) {
            case "emojis":  sym = EMOJI_MAP.get(filename);  break;
            case "animals": sym = ANIMAL_MAP.get(filename); break;
            case "fruits":  sym = FRUIT_MAP.get(filename);  break;
            case "shapes":  sym = SHAPE_MAP.get(filename);  break;
        }
        // Fallback: use first letter of filename as symbol
        if (sym == null || sym.isEmpty()) {
            sym = filename.replace(".png", "").replace(".jpg", "")
                         .substring(0, 1).toUpperCase();
        }
        return sym;
    }

    /**
     * Render a Unicode emoji or symbol into an ImageIcon of the given size.
     * Uses "Segoe UI Emoji" font on Windows for best emoji rendering.
     */
    private static ImageIcon renderSymbolAsIcon(String symbol, int size, String category) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);

        // Transparent background (card face will provide background)
        g2.setColor(new Color(0, 0, 0, 0));
        g2.fillRect(0, 0, size, size);

        // Choose font — emoji font for emoji/animal/fruit, plain for shapes
        Font font;
        int fontSize = (int)(size * 0.62);
        if (category.equalsIgnoreCase("shapes")) {
            font = new Font("Segoe UI", Font.BOLD, fontSize);
            g2.setColor(new Color(50, 80, 160));
        } else {
            // Try Segoe UI Emoji first (Windows), fallback to system default
            font = new Font("Segoe UI Emoji", Font.PLAIN, fontSize);
            g2.setColor(new Color(30, 30, 30));
        }
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(symbol);
        int textH = fm.getAscent();
        int x = (size - textW) / 2;
        int y = (size - fm.getHeight()) / 2 + textH;

        g2.drawString(symbol, x, y);
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
                for (int i = 0; i < files.length; i++) names[i] = files[i].getName();
                return names;
            }
        }
        return getFallbackNames(category);
    }

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

    public static void clearCache() { cache.clear(); }
}