package me.foesio.foChatGames.util;

import me.foesio.core.text.FoText;

import java.util.Locale;
import java.util.Map;

public final class TextUtil {
    private TextUtil() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }

        String withHex = replaceMiniMessageColors(input);
        return FoText.color(withHex);
    }

    public static String strip(String input) {
        return FoText.stripColor(color(input));
    }

    public static String tokens(String input, Map<String, String> tokens) {
        String output = input == null ? "" : input;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return output;
    }

    public static String smallCaps(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char character : input.toLowerCase(Locale.ROOT).toCharArray()) {
            builder.append(smallCap(character));
        }
        return builder.toString();
    }

    private static String replaceMiniMessageColors(String input) {
        return input
                .replaceAll("(?i)<#([0-9a-f]{6})>", "&#$1")
                .replaceAll("(?i)</[^>]+>", "")
                .replaceAll("(?i)<reset>", "&r")
                .replaceAll("(?i)<white>", "&f")
                .replaceAll("(?i)<gray>", "&7")
                .replaceAll("(?i)<dark_gray>", "&8")
                .replaceAll("(?i)<green>", "&a")
                .replaceAll("(?i)<red>", "&c")
                .replaceAll("(?i)<yellow>", "&e")
                .replaceAll("(?i)<aqua>", "&b");
    }

    private static String smallCap(char character) {
        return switch (character) {
            case 'a' -> "ᴀ";
            case 'b' -> "ʙ";
            case 'c' -> "ᴄ";
            case 'd' -> "ᴅ";
            case 'e' -> "ᴇ";
            case 'f' -> "ғ";
            case 'g' -> "ɢ";
            case 'h' -> "ʜ";
            case 'i' -> "ɪ";
            case 'j' -> "ᴊ";
            case 'k' -> "ᴋ";
            case 'l' -> "ʟ";
            case 'm' -> "ᴍ";
            case 'n' -> "ɴ";
            case 'o' -> "ᴏ";
            case 'p' -> "ᴘ";
            case 'q' -> "ǫ";
            case 'r' -> "ʀ";
            case 's' -> "s";
            case 't' -> "ᴛ";
            case 'u' -> "ᴜ";
            case 'v' -> "ᴠ";
            case 'w' -> "ᴡ";
            case 'x' -> "x";
            case 'y' -> "ʏ";
            case 'z' -> "ᴢ";
            default -> String.valueOf(character);
        };
    }
}
