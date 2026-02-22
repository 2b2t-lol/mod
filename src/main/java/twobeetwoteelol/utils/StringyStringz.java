package twobeetwoteelol.utils;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import twobeetwoteelol.model.ExclusionZone;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class StringyStringz {
    public static String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return null;
        }

        String url = raw.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        return null;
    }

    public static String[] toLines(Text[] textLines) {
        String[] lines = new String[4];

        for (int i = 0; i < lines.length; i++) {
            if (textLines != null && i < textLines.length && textLines[i] != null) {
                lines[i] = normalizeLine(textLines[i].getString());
            } else {
                lines[i] = "";
            }
        }

        return lines;
    }

    public static boolean hasContent(String[] lines) {
        for (String line : lines) {
            if (!line.isBlank()) {
                return true;
            }
        }

        return false;
    }

    public static String buildPositionKey(String dimension, BlockPos pos) {
        return dimension + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ();
    }

    public static String buildSignature(String[] lines) {
        return String.join("\\u241F", lines);
    }

    public static String seenHash(String content, String dimension, BlockPos pos) {
        String raw = content + "-" + buildPositionKey(dimension, pos);
        return sha256Hex(raw);
    }

    public static List<ExclusionZone> parseExclusionZones(List<String> rawEntries, int defaultRadius) {
        List<ExclusionZone> zones = new ArrayList<>();
        int clampedDefaultRadius = Math.max(0, defaultRadius);

        for (String rawEntry : rawEntries) {
            if (rawEntry == null || rawEntry.isBlank()) {
                continue;
            }

            String entry = rawEntry.trim().replace('|', ',');
            String[] parts = entry.split("\\s*,\\s*");
            Integer x;
            Integer z;
            Integer radius;

            if (parts.length == 3) {
                x = parseInteger(parts[0]);
                z = parseInteger(parts[1]);
                radius = parseInteger(parts[2]);
            } else {
                continue;
            }

            if (x == null || z == null) {
                continue;
            }

            int resolvedRadius = radius == null ? clampedDefaultRadius : Math.max(0, radius);
            zones.add(new ExclusionZone(x, z, resolvedRadius));
        }

        return zones;
    }

    public static ExclusionEntryText parseExclusionEntry(String raw, int defaultRange) {
        String fallbackRange = Integer.toString(Math.max(0, defaultRange));
        if (raw == null || raw.isBlank()) {
            return new ExclusionEntryText("", "", fallbackRange);
        }

        String[] parts = raw.trim().replace('|', ',').split("\\s*,\\s*");

        if (parts.length == 3) {
            return new ExclusionEntryText(parts[0], parts[1], parts[2]);
        }

        return new ExclusionEntryText("", "", fallbackRange);
    }

    public static String formatExclusionEntry(String x, String z, String range) {
        String xPart = x == null ? "" : x.trim();
        String zPart = z == null ? "" : z.trim();
        String rangePart = range == null ? "" : range.trim();
        return xPart + "," + zPart + "," + rangePart;
    }

    private static String normalizeLine(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\u0000", "").trim();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            char[] hexChars = new char[hash.length * 2];
            char[] alphabet = "0123456789abcdef".toCharArray();

            for (int i = 0; i < hash.length; i++) {
                int unsigned = hash[i] & 0xFF;
                hexChars[i * 2] = alphabet[unsigned >>> 4];
                hexChars[(i * 2) + 1] = alphabet[unsigned & 0x0F];
            }

            return new String(hexChars);
        } catch (NoSuchAlgorithmException ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static Integer parseInteger(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    public record ExclusionEntryText(String x, String z, String range) {
    }
}
