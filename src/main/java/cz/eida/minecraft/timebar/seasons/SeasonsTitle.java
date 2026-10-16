package cz.eida.minecraft.timebar.seasons;

import cz.eida.minecraft.timebar.calendar.CalendarDay;
import cz.eida.minecraft.timebar.calendar.DatePattern;
import cz.eida.minecraft.timebar.util.LegacyColors;
import cz.eida.minecraft.timebar.util.RomanNumeral;
import lol.hyper.timebar.utils.NumberFormat;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Bossbar title, time of day and color from {@code realisticseasons.yml} (shared by RealisticSeasons
 * and OpenSeasons) for one {@link CalendarDay}.
 *
 * <p>A month's section {@code month.<key>} is found by the calendar's month name, then by its number
 * ({@code month.'7'}), then, in a 12-month calendar, by the English Gregorian name ({@code month.july}),
 * so the old config keeps working when OpenSeasons shows Czech month names. Times of day fall back
 * to {@code month.default}, then to built-in times.
 *
 * <p>{@code MMMM} in {@code date-format} is {@code date-name} of the month's section; without it a
 * Gregorian month (12-month calendar, named in English or in {@code date-locale}) gets the locale's
 * form ("8. července"), any other month its display name. Problems are logged once, not every tick.
 */
public final class SeasonsTitle implements DatePattern.Names {

    static final String DEFAULT_TITLE = "{TIME} - {TIME-WORD} ({DATE}) - {SEASON}";
    private static final String[] TIMES = {"midnight", "dawn", "morning", "noon", "afternoon", "sunset", "night"};
    private static final Map<String, String> DEFAULT_TIMES = Map.of(
            "midnight", "00:00", "dawn", "05:00", "morning", "07:30", "noon", "12:00",
            "afternoon", "16:00", "sunset", "17:45", "night", "19:15");

    private final ConfigurationSection config;
    private final Logger logger;
    private final Locale locale;
    private final DatePattern datePattern;
    private final Set<String> warned = new HashSet<>();

    public SeasonsTitle(ConfigurationSection config, Logger logger) {
        this.config = config;
        this.logger = logger;
        String tag = config.getString("date-locale");
        this.locale = tag == null || tag.isBlank() ? Locale.getDefault(Locale.Category.FORMAT) : Locale.forLanguageTag(tag);
        this.datePattern = datePattern(config.getString("date-format"));
    }

    private DatePattern datePattern(String format) {
        // if these config values are missing, use the default ones
        if (format == null) {
            logger.warning("date-format is missing! Using default American English format.");
            return DatePattern.parse("M/dd/yyyy");
        }
        try {
            DatePattern pattern = DatePattern.parse(format);
            if (!pattern.unsupported().isEmpty()) {
                logger.warning("date-format '" + format + "': letters " + pattern.unsupported()
                        + " are not supported and stay as text (use d, M, y, E).");
            }
            return pattern;
        } catch (IllegalArgumentException exception) {
            logger.warning("date-format is NOT a valid format! Using default American English format. " + exception.getMessage());
            return DatePattern.parse("M/d/yyyy");
        }
    }

    public Locale locale() {
        return locale;
    }

    /** title with all placeholders but PlaceholderAPI ones */
    public String title(CalendarDay day, String timeOfDay) {
        ConfigurationSection month = monthSection(day);
        // if this month has a custom title, use it instead
        String title = month == null ? null : month.getString("title");
        if (title == null) {
            title = config.getString("timebar-title");
        }
        if (title == null) {
            warnOnce("timebar-title", "timebar-title is not set! Using default.");
            title = DEFAULT_TITLE;
        }
        String time = String.format("%02d:%02d", day.hours(), day.minutes());
        if (!config.getBoolean("use-24h-format")) {
            time = LocalTime.of(day.hours(), day.minutes()).format(DateTimeFormatter.ofPattern("hh:mm a", locale));
        }
        return title
                .replace("{TIME}", time)
                .replace("{TIME-WORD}", timeOfDay)
                .replace("{DAYCOUNT}", NumberFormat.formatInt((int) day.dayCount()))
                .replace("{WORLDYEAR-ROMAN}", RomanNumeral.toRoman(day.worldYear()))
                .replace("{WORLDYEAR}", Integer.toString(day.worldYear()))
                .replace("{SEASON}", seasonName(day) + "<reset>")
                .replace("{DATE}", datePattern.format(day, this))
                .replace("{DAY}", weekday(day))
                .replace("{MONTH}", monthName(day))
                .replace("{DAYPERCENT}", String.format("%.2f", day.dayProgress() * 100) + "%");
    }

    /** "time of day" word for the month's times */
    public String timeOfDay(CalendarDay day) {
        ConfigurationSection month = monthSection(day);
        if (month == null) {
            month = child(config.getConfigurationSection("month"), "default");
        }
        String key = "month." + day.monthName();
        String where = month == null ? "built-in times" : "month." + month.getName();
        if (month == null) {
            warnOnce(key, "Section " + key + " (month " + day.month() + ") does NOT EXIST! Using default times of day.");
        }
        LocalTime[] at = new LocalTime[TIMES.length];
        for (int i = 0; i < TIMES.length; i++) {
            String value = month == null ? null : month.getString(TIMES[i]);
            if (value == null) {
                if (month != null) {
                    warnOnce(key + "." + TIMES[i], where + "." + TIMES[i] + " is NOT SET! Using " + DEFAULT_TIMES.get(TIMES[i]) + ".");
                }
                value = DEFAULT_TIMES.get(TIMES[i]);
            }
            try {
                at[i] = LocalTime.parse(value);
            } catch (DateTimeParseException exception) {
                warnOnce(key + "." + TIMES[i] + ".invalid", where + "." + TIMES[i] + " '" + value + "' is not a time like 06:30!");
                return "INVALID";
            }
        }
        LocalTime now = LocalTime.of(day.hours(), day.minutes());
        for (int i = 0; i < TIMES.length - 1; i++) {
            if (!now.isBefore(at[i]) && now.isBefore(at[i + 1])) {
                return word(TIMES[i]);
            }
        }
        if (!now.isBefore(at[TIMES.length - 1])) {
            return word("night");
        }
        warnOnce(key + ".order", "Unable to find suitable time for " + now + " in " + where);
        return "INVALID";
    }

    /** {@code colors.<SEASON>}, otherwise the fallback */
    public BossBar.Color color(CalendarDay day, BossBar.Color fallback) {
        String name = config.getString("colors." + day.seasonKey());
        if (name == null) {
            return fallback;
        }
        try {
            return BossBar.Color.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            warnOnce("colors." + day.seasonKey(), name + " is not a valid color for TimeBar.");
            return fallback;
        }
    }

    /** {@code seasons.<SEASON>} or the plugin's display name, as MiniMessage */
    public String seasonName(CalendarDay day) {
        String name = config.getString("seasons." + day.seasonKey());
        return LegacyColors.toMiniMessage(name != null ? name : day.seasonName());
    }

    /** {MONTH}: {@code name} of the month's section, otherwise the calendar's name */
    public String monthName(CalendarDay day) {
        if ("DISABLED".equals(day.monthName())) {
            return "DISABLED";
        }
        ConfigurationSection month = monthSection(day);
        String name = month == null ? null : month.getString("name");
        return name != null ? name : day.monthName();
    }

    @Override
    public String monthFull(CalendarDay day) {
        ConfigurationSection month = monthSection(day);
        String dateName = month == null ? null : month.getString("date-name");
        if (dateName != null) {
            return dateName;
        }
        Month gregorian = gregorianMonth(day);
        return gregorian != null ? gregorian.getDisplayName(TextStyle.FULL, locale) : monthName(day);
    }

    @Override
    public String monthShort(CalendarDay day) {
        Month gregorian = gregorianMonth(day);
        return gregorian != null ? gregorian.getDisplayName(TextStyle.SHORT, locale) : DatePattern.shortName(monthName(day));
    }

    /** {DAY}: {@code days.<weekday>} or the calendar's weekday */
    @Override
    public String weekday(CalendarDay day) {
        ConfigurationSection days = child(config, "days");
        String key = days == null ? null : key(days, day.weekday());
        String name = key == null ? null : days.getString(key);
        return name != null ? name : day.weekday();
    }

    /** {@code month.<name>}, {@code month.<number>}, in a 12-month calendar {@code month.<english name>} */
    ConfigurationSection monthSection(CalendarDay day) {
        ConfigurationSection months = config.getConfigurationSection("month");
        if (months == null) {
            return null;
        }
        ConfigurationSection section = child(months, day.monthName());
        if (section == null) {
            section = child(months, Integer.toString(day.month()));
        }
        Month gregorian = gregorianNumber(day);
        if (section == null && gregorian != null) {
            section = child(months, gregorian.name());
        }
        return section;
    }

    /** month of a 12-month calendar named like the Gregorian one, in English or in {@code date-locale} */
    private Month gregorianMonth(CalendarDay day) {
        Month month = gregorianNumber(day);
        if (month == null) {
            return null;
        }
        String name = day.monthName();
        boolean gregorianName = name.equalsIgnoreCase(month.name())
                || name.equalsIgnoreCase(month.getDisplayName(TextStyle.FULL_STANDALONE, locale))
                || name.equalsIgnoreCase(month.getDisplayName(TextStyle.FULL, locale));
        return gregorianName ? month : null;
    }

    private static Month gregorianNumber(CalendarDay day) {
        boolean twelveMonths = day.monthCount() == 0 || day.monthCount() == 12;
        return twelveMonths && day.month() >= 1 && day.month() <= 12 ? Month.of(day.month()) : null;
    }

    private String word(String time) {
        String word = config.getString("times." + time);
        return word != null ? word : time;
    }

    /** child section by key regardless of case, so "July" finds {@code july:} and "Červenec" {@code červenec:} */
    private static ConfigurationSection child(ConfigurationSection parent, String key) {
        if (parent == null || key == null) {
            return null;
        }
        String found = key(parent, key);
        return found == null ? null : parent.getConfigurationSection(found);
    }

    private static String key(ConfigurationSection parent, String key) {
        if (key == null) {
            return null;
        }
        for (String candidate : parent.getKeys(false)) {
            if (candidate.equalsIgnoreCase(key)) {
                return candidate;
            }
        }
        return null;
    }

    private void warnOnce(String key, String message) {
        if (warned.add(key)) {
            logger.warning(message);
        }
    }
}
