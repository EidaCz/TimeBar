package cz.eida.minecraft.timebar.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RomanNumeralTest {

    @Test
    void romanNumerals() {
        assertEquals("I", RomanNumeral.toRoman(1));
        assertEquals("IV", RomanNumeral.toRoman(4));
        assertEquals("IX", RomanNumeral.toRoman(9));
        assertEquals("XIV", RomanNumeral.toRoman(14));
        assertEquals("CDXLIX", RomanNumeral.toRoman(449));
        assertEquals("CDLIII", RomanNumeral.toRoman(453));
        assertEquals("MCMXCIV", RomanNumeral.toRoman(1994));
        assertEquals("MMMCMXCIX", RomanNumeral.toRoman(3999));
    }

    /** a world younger than one year: fullTime / 24000 / 365 == 0 used to throw NullPointerException */
    @Test
    void yearsBelowOneStayArabic() {
        assertEquals("0", RomanNumeral.toRoman(0));
        assertEquals("-3", RomanNumeral.toRoman(-3));
        assertEquals(Integer.toString(Integer.MIN_VALUE), RomanNumeral.toRoman(Integer.MIN_VALUE));
    }
}
