package cz.eida.minecraft.timebar.seasons;

import cz.eida.minecraft.timebar.calendar.CalendarDay;
import lol.hyper.timebar.utils.NumberFormat;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Title over the MC-215 production {@code plugins/TimeBar/realisticseasons.yml} (copied 16. 10. 2026)
 * with the server's JVM locale cs_CZ: today it shows "Léto | Sobota 8. července CDLIII, 14:20 | Poledne".
 */
class SeasonsTitleTest {

    private static final String PRODUCTION_TITLE = "Léto<reset> | Sobota 8. července CDLIII, 14:20 | Poledne";

    private Locale savedLocale;
    private final List<String> warnings = new ArrayList<>();
    private final Logger logger = Logger.getAnonymousLogger();

    @BeforeEach
    void czechServer() {
        savedLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("cs-CZ"));
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                warnings.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(Locale.Category.FORMAT, savedLocale);
    }

    private static YamlConfiguration config(String resource) {
        InputStream in = SeasonsTitleTest.class.getResourceAsStream(resource);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private static YamlConfiguration production() {
        return config("/mc215-realisticseasons.yml");
    }

    /** RealisticSeasons: calendar.yml with English months, Czech weekdays, year of the world's age */
    private static CalendarDay realisticSeasons(int worldYear) {
        return new CalendarDay("SUMMER", "Léto", 8, 7, 0, "July", 449, "Sobota", worldYear, worldYear * 365L, 14, 20, 0);
    }

    /** OpenSeasons with its names.yml: Czech months, 12-month calendar */
    private static CalendarDay openSeasons(int year) {
        return new CalendarDay("SUMMER", "Léto", 8, 7, 12, "Červenec", year, "Sobota", year, year * 365L, 14, 20, 0);
    }

    private static String title(SeasonsTitle title, CalendarDay day) {
        return title.title(day, title.timeOfDay(day));
    }

    @Test
    void productionConfigWithRealisticSeasons() {
        SeasonsTitle title = new SeasonsTitle(production(), logger);
        assertEquals(PRODUCTION_TITLE, title(title, realisticSeasons(453)));
        assertEquals("Červenec", title.monthName(realisticSeasons(453)));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    /** names.yml of OpenSeasons renames July to Červenec; the July section and "července" still apply */
    @Test
    void productionConfigWithOpenSeasonsCzechMonths() {
        SeasonsTitle title = new SeasonsTitle(production(), logger);
        assertEquals(PRODUCTION_TITLE, title(title, openSeasons(453)));
        assertEquals("Červenec", title.monthName(openSeasons(453)));
        assertEquals("Odpoledne", title.timeOfDay(new CalendarDay("SUMMER", "Léto", 8, 7, 12, "Červenec", 453, "Sobota",
                453, 0, 18, 30, 0)));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void youngWorldHasNoRomanYear() {
        SeasonsTitle title = new SeasonsTitle(production(), logger);
        assertEquals("Léto<reset> | Sobota 8. července 0, 14:20 | Poledne", title(title, openSeasons(0)));
        assertEquals("Léto<reset> | Sobota 8. července 0, 14:20 | Poledne", title(title, realisticSeasons(0)));
    }

    @Test
    void seasonColors() {
        SeasonsTitle title = new SeasonsTitle(production(), logger);
        assertEquals(BossBar.Color.YELLOW, title.color(openSeasons(453), BossBar.Color.BLUE));
        CalendarDay disabled = new CalendarDay("DISABLED", "DISABLED", 8, 7, 12, "Červenec", 453, "Sobota", 453, 0, 14, 20, 0);
        assertEquals(BossBar.Color.BLUE, title.color(disabled, BossBar.Color.BLUE));
    }

    /** 10 months with own names: no Gregorian guess, built-in times, one warning for the month */
    @Test
    void customCalendarWithoutSections() {
        SeasonsTitle title = new SeasonsTitle(production(), logger);
        CalendarDay day = new CalendarDay("WINTER", "&bZima", 40, 3, 10, "Mrazník", 7, "Třetidenník", 7, 0, 3, 5, 0);
        assertEquals("<aqua>Zima<reset> | Třetidenník 40. Mrazník VII, 03:05 | Půlnoc", title(title, day));
        assertEquals("Mrazník", title.monthName(day));
        title(title, day);
        assertEquals(1, warnings.size(), warnings.toString());
    }

    /** a custom month gets its section by name or by number, with date-name for {DATE} */
    @Test
    void customCalendarSections() {
        YamlConfiguration config = production();
        config.set("month.mrazník.name", "Mrazník (3.)");
        config.set("month.mrazník.date-name", "mrazníku");
        config.set("month.mrazník.title", "{DATE} – {MONTH}");
        config.set("month.4.name", "Tání");
        config.set("month.default.dawn", "07:00");
        SeasonsTitle title = new SeasonsTitle(config, logger);
        CalendarDay mraznik = new CalendarDay("WINTER", "Zima", 40, 3, 10, "Mrazník", 7, "Třetidenník", 7, 0, 3, 5, 0);
        assertEquals("40. mrazníku – Mrazník (3.)", title(title, mraznik));
        CalendarDay tani = new CalendarDay("SPRING", "Jaro", 2, 4, 10, "Tajík", 7, "Prvník", 7, 0, 6, 30, 0);
        assertEquals("Tání", title.monthName(tani));
        // no times in month.4: built-in ones (dawn 05:00, morning 07:30)
        assertEquals("Úsvit", title.timeOfDay(tani));
        CalendarDay other = new CalendarDay("SPRING", "Jaro", 2, 5, 10, "Kvetník", 7, "Prvník", 7, 0, 6, 30, 0);
        // month.default dawn 07:00: still midnight
        assertEquals("Půlnoc", title.timeOfDay(other));
    }

    @Test
    void dateNameAndOverrides() {
        YamlConfiguration config = production();
        config.set("month.july.date-name", "července L. P.");
        config.set("seasons.SUMMER", "&eLéto");
        SeasonsTitle title = new SeasonsTitle(config, logger);
        assertEquals("<yellow>Léto<reset> | Sobota 8. července L. P. CDLIII, 14:20 | Poledne", title(title, openSeasons(453)));
        // days: of the production config translates English weekday names
        CalendarDay english = new CalendarDay("SUMMER", "Léto", 8, 7, 12, "July", 453, "Saturday", 453, 0, 14, 20, 0);
        assertEquals("Sobota", title.weekday(english));
    }

    @Test
    void dateLocale() {
        YamlConfiguration config = production();
        config.set("date-locale", "en");
        config.set("use-24h-format", false);
        config.set("timebar-title", "{DATE} {TIME} {WORLDYEAR} {DAYCOUNT}");
        SeasonsTitle title = new SeasonsTitle(config, logger);
        assertEquals("8. July 02:20 PM 453 " + NumberFormat.formatInt(165345), title(title, realisticSeasons(453)));
    }

    /** the config TimeBar writes for a new server */
    @Test
    void bundledConfig() {
        SeasonsTitle title = new SeasonsTitle(config("/realisticseasons.yml"), logger);
        CalendarDay day = new CalendarDay("FALL", "Podzim", 1, 10, 12, "Říjen", 2, "Pondělí", 2, 1000, 8, 0, 0);
        assertEquals("Podzim<reset> | Pondělí 1. října II, 08:00 | Morning", title(title, day));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }
}
