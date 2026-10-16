package cz.eida.minecraft.timebar.calendar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@code date-format} over a custom calendar. Letters work like in {@code DateTimeFormatter}, but
 * the values come from the world's calendar, not from a Gregorian {@code LocalDate}:
 * <ul>
 *     <li>{@code d}, {@code dd} – day of the month</li>
 *     <li>{@code M}, {@code MM} – month number; {@code MMM} short name, {@code MMMM} full name,
 *     {@code MMMMM} first letter</li>
 *     <li>{@code y}, {@code u} – year; {@code yy} last two digits, {@code yyyy} at least four digits</li>
 *     <li>{@code E}–{@code EEE} short weekday, {@code EEEE} full weekday, {@code EEEEE} first letter</li>
 *     <li>{@code 'text'} literal text, {@code ''} an apostrophe</li>
 * </ul>
 * Other letters are kept as they are and reported by {@link #unsupported()}.
 */
public final class DatePattern {

    /** names the pattern needs; the caller resolves them from its config and the calendar */
    public interface Names {
        String monthFull(CalendarDay day);

        String monthShort(CalendarDay day);

        String weekday(CalendarDay day);
    }

    private sealed interface Part permits Literal, Field {
    }

    private record Literal(String text) implements Part {
    }

    private record Field(char letter, int count) implements Part {
    }

    private final String pattern;
    private final List<Part> parts;
    private final Set<Character> unsupported;

    private DatePattern(String pattern, List<Part> parts, Set<Character> unsupported) {
        this.pattern = pattern;
        this.parts = List.copyOf(parts);
        this.unsupported = Set.copyOf(unsupported);
    }

    /** @throws IllegalArgumentException for a quote that is not closed */
    public static DatePattern parse(String pattern) {
        List<Part> parts = new ArrayList<>();
        Set<Character> unsupported = new TreeSet<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '\'') {
                if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                    literal.append('\'');
                    i += 2;
                    continue;
                }
                int end = i + 1;
                StringBuilder quoted = new StringBuilder();
                while (true) {
                    if (end >= pattern.length()) {
                        throw new IllegalArgumentException("unclosed quote in date format: " + pattern);
                    }
                    if (pattern.charAt(end) == '\'') {
                        if (end + 1 < pattern.length() && pattern.charAt(end + 1) == '\'') {
                            quoted.append('\'');
                            end += 2;
                            continue;
                        }
                        break;
                    }
                    quoted.append(pattern.charAt(end++));
                }
                literal.append(quoted);
                i = end + 1;
                continue;
            }
            if ("dMyuE".indexOf(c) >= 0) {
                int count = 1;
                while (i + count < pattern.length() && pattern.charAt(i + count) == c) {
                    count++;
                }
                if (!literal.isEmpty()) {
                    parts.add(new Literal(literal.toString()));
                    literal.setLength(0);
                }
                parts.add(new Field(c, count));
                i += count;
                continue;
            }
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                unsupported.add(c);
            }
            literal.append(c);
            i++;
        }
        if (!literal.isEmpty()) {
            parts.add(new Literal(literal.toString()));
        }
        return new DatePattern(pattern, parts, unsupported);
    }

    public String format(CalendarDay day, Names names) {
        StringBuilder out = new StringBuilder();
        for (Part part : parts) {
            switch (part) {
                case Literal literal -> out.append(literal.text());
                case Field field -> out.append(field(field, day, names));
            }
        }
        return out.toString();
    }

    /** letters the pattern has but this format does not know (kept as literal text) */
    public Set<Character> unsupported() {
        return unsupported;
    }

    public String pattern() {
        return pattern;
    }

    private static String field(Field field, CalendarDay day, Names names) {
        int count = field.count();
        return switch (field.letter()) {
            case 'd' -> pad(day.day(), count);
            case 'M' -> switch (count) {
                case 1, 2 -> pad(day.month(), count);
                case 3 -> names.monthShort(day);
                case 4 -> names.monthFull(day);
                default -> firstLetter(names.monthFull(day));
            };
            case 'y', 'u' -> count == 2 ? pad(Math.floorMod(day.year(), 100), 2) : pad(day.year(), count);
            case 'E' -> switch (count) {
                case 1, 2, 3 -> shortName(names.weekday(day));
                case 4 -> names.weekday(day);
                default -> firstLetter(names.weekday(day));
            };
            default -> throw new IllegalStateException("unknown field " + field.letter());
        };
    }

    private static String pad(int value, int width) {
        String digits = Integer.toString(Math.abs(value));
        StringBuilder sb = new StringBuilder();
        if (value < 0) {
            sb.append('-');
        }
        sb.append("0".repeat(Math.max(0, width - digits.length())));
        return sb.append(digits).toString();
    }

    /** short form of a name from a custom calendar: first three letters */
    public static String shortName(String name) {
        return name.codePointCount(0, name.length()) <= 3 ? name : name.substring(0, name.offsetByCodePoints(0, 3));
    }

    private static String firstLetter(String name) {
        return name.isEmpty() ? name : name.substring(0, name.offsetByCodePoints(0, 1));
    }
}
