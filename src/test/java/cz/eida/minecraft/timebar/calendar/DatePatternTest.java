package cz.eida.minecraft.timebar.calendar;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatePatternTest {

    private static final DatePattern.Names NAMES = new DatePattern.Names() {
        @Override
        public String monthFull(CalendarDay day) {
            return day.monthName();
        }

        @Override
        public String monthShort(CalendarDay day) {
            return DatePattern.shortName(day.monthName());
        }

        @Override
        public String weekday(CalendarDay day) {
            return day.weekday();
        }
    };

    private static CalendarDay day(int day, int month, String monthName, int year, String weekday) {
        return new CalendarDay("SUMMER", "Léto", day, month, 0, monthName, year, weekday, year, 0, 12, 0, 0);
    }

    @Test
    void numbersAndNames() {
        CalendarDay day = day(8, 7, "Červenec", 453, "Sobota");
        assertEquals("8. Červenec", DatePattern.parse("d. MMMM").format(day, NAMES));
        assertEquals("7/08/0453", DatePattern.parse("M/dd/yyyy").format(day, NAMES));
        assertEquals("08.07.53", DatePattern.parse("dd.MM.yy").format(day, NAMES));
        assertEquals("453-7-8", DatePattern.parse("y-M-d").format(day, NAMES));
        assertEquals("Sob, Sobota 8. Čer", DatePattern.parse("E, EEEE d. MMM").format(day, NAMES));
        assertEquals("S Č", DatePattern.parse("EEEEE MMMMM").format(day, NAMES));
    }

    /** a custom calendar: month 14 of 15, any names */
    @Test
    void customCalendar() {
        CalendarDay day = day(40, 14, "Mrazník", 3, "Třetidenník");
        assertEquals("40. Mrazník 3 (Třetidenník)", DatePattern.parse("d. MMMM y (EEEE)").format(day, NAMES));
        assertEquals("40/14/0003", DatePattern.parse("dd/MM/yyyy").format(day, NAMES));
    }

    @Test
    void negativeYear() {
        assertEquals("-12", DatePattern.parse("y").format(day(1, 1, "Leden", -12, "Pondělí"), NAMES));
        assertEquals("-0012", DatePattern.parse("yyyy").format(day(1, 1, "Leden", -12, "Pondělí"), NAMES));
    }

    @Test
    void quotedText() {
        CalendarDay day = day(8, 7, "Červenec", 453, "Sobota");
        assertEquals("8. den, rok 453", DatePattern.parse("d'. den, rok' y").format(day, NAMES));
        assertEquals("o'clock 8", DatePattern.parse("'o''clock' d").format(day, NAMES));
        assertEquals("it's 8", DatePattern.parse("'it''s' d").format(day, NAMES));
        assertEquals("'8'", DatePattern.parse("''d''").format(day, NAMES));
    }

    @Test
    void unsupportedLettersStayAsText() {
        DatePattern pattern = DatePattern.parse("d. MMMM G");
        assertEquals(Set.of('G'), pattern.unsupported());
        assertEquals("8. Červenec G", pattern.format(day(8, 7, "Červenec", 453, "Sobota"), NAMES));
        assertTrue(DatePattern.parse("d. MMMM").unsupported().isEmpty());
    }

    @Test
    void unclosedQuoteIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DatePattern.parse("d 'den"));
    }
}
