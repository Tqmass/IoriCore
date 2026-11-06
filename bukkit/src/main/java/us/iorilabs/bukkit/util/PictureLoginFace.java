package us.iorilabs.bukkit.util;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public final class PictureLoginFace {

    private static final char BLOCK = '\u2588';
    private static final int SIZE = 8;

    private PictureLoginFace() {}

    public static BufferedImage fetch(String url) throws IOException {
        if (url == null || url.isEmpty()) throw new IOException("empty url");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(4000);
        connection.setReadTimeout(4000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; IoriCore/1.0; +https://royalctw.net)");
        connection.connect();
        try (InputStream in = connection.getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IOException("unreadable image");
            return image;
        } finally {
            connection.disconnect();
        }
    }

    public static List<String> compose(BufferedImage image, List<String> sideText, boolean hex) {
        Color[][] pixels = pixels(image, SIZE);
        List<String> out = new ArrayList<>(SIZE);
        for (int y = 0; y < SIZE; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < SIZE; x++) {
                Color color = pixels[x][y];
                if (color == null || color.getAlpha() < 32) {
                    line.append(' ');
                    continue;
                }
                line.append(hex ? toHex(color) : closest(color).toString());
                line.append(BLOCK);
            }
            line.append(org.bukkit.ChatColor.RESET);
            if (sideText != null && y < sideText.size() && sideText.get(y) != null && !sideText.get(y).isEmpty()) {
                line.append(' ').append(sideText.get(y));
            }
            out.add(line.toString());
        }
        return out;
    }

    private static Color[][] pixels(BufferedImage image, int height) {
        double ratio = (double) image.getHeight() / Math.max(1, image.getWidth());
        int width = (int) Math.round(height / ratio);
        if (width < 1) width = 1;
        if (width > 10) width = 10;
        BufferedImage resized = resize(image, width, height);
        Color[][] out = new Color[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out[x][y] = new Color(resized.getRGB(x, y), true);
            }
        }
        return out;
    }

    private static BufferedImage resize(BufferedImage original, int width, int height) {
        AffineTransform transform = new AffineTransform();
        transform.scale(width / (double) original.getWidth(), height / (double) original.getHeight());
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(original, null);
    }

    private static String toHex(Color color) {
        StringBuilder builder = new StringBuilder("§x");
        String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        for (char c : hex.toCharArray()) builder.append('§').append(c);
        return builder.toString();
    }

    private static org.bukkit.ChatColor closest(Color color) {
        org.bukkit.ChatColor best = org.bukkit.ChatColor.WHITE;
        double bestDistance = Double.MAX_VALUE;
        for (org.bukkit.ChatColor chat : org.bukkit.ChatColor.values()) {
            if (chat.isFormat() || chat == org.bukkit.ChatColor.RESET) continue;
            Color mapped = map(chat);
            if (mapped == null) continue;
            double distance = distance(color, mapped);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = chat;
            }
        }
        return best;
    }

    private static Color map(org.bukkit.ChatColor color) {
        switch (color) {
            case BLACK:
                return new Color(0, 0, 0);
            case DARK_BLUE:
                return new Color(0, 0, 170);
            case DARK_GREEN:
                return new Color(0, 170, 0);
            case DARK_AQUA:
                return new Color(0, 170, 170);
            case DARK_RED:
                return new Color(170, 0, 0);
            case DARK_PURPLE:
                return new Color(170, 0, 170);
            case GOLD:
                return new Color(255, 170, 0);
            case GRAY:
                return new Color(170, 170, 170);
            case DARK_GRAY:
                return new Color(85, 85, 85);
            case BLUE:
                return new Color(85, 85, 255);
            case GREEN:
                return new Color(85, 255, 85);
            case AQUA:
                return new Color(85, 255, 255);
            case RED:
                return new Color(255, 85, 85);
            case LIGHT_PURPLE:
                return new Color(255, 85, 255);
            case YELLOW:
                return new Color(255, 255, 85);
            case WHITE:
                return new Color(255, 255, 255);
            default:
                return null;
        }
    }

    private static double distance(Color a, Color b) {
        long rmean = ((long) a.getRed() + b.getRed()) / 2L;
        long r = a.getRed() - b.getRed();
        long g = a.getGreen() - b.getGreen();
        long bl = a.getBlue() - b.getBlue();
        return Math.sqrt((((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * bl * bl) >> 8));
    }
}
