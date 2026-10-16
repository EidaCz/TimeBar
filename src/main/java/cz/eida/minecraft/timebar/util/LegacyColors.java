package cz.eida.minecraft.timebar.util;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Legacy color codes ({@code &a}, {@code §l}, {@code &#RRGGBB}) to MiniMessage tags. Season
 * names from RealisticSeasons and OpenSeasons come with them.
 */
public final class LegacyColors {

    private static final Pattern CODE = Pattern.compile("[&§](?:#([0-9a-f]{6})|([0-9a-fk-or]))", Pattern.CASE_INSENSITIVE);
    private static final Map<Character, String> TAGS = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('k', "obf"),
            Map.entry('l', "b"),
            Map.entry('m', "st"),
            Map.entry('n', "u"),
            Map.entry('o', "i"),
            Map.entry('r', "reset")
    );

    private LegacyColors() {
    }

    public static String toMiniMessage(String text) {
        Matcher matcher = CODE.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String tag = matcher.group(1) != null
                    ? "#" + matcher.group(1).toLowerCase(Locale.ROOT)
                    : TAGS.get(Character.toLowerCase(matcher.group(2).charAt(0)));
            matcher.appendReplacement(sb, Matcher.quoteReplacement("<" + tag + ">"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
