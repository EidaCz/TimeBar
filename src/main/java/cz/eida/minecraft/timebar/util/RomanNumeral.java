package cz.eida.minecraft.timebar.util;

/**
 * Roman numerals extension
 *
 * @author EidaCz
 */
public final class RomanNumeral {

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private RomanNumeral() {
    }

    /**
     * Roman numeral of a positive number. Numbers below 1 (a world younger than one year, a date
     * before year 1) have no Roman form and are returned in Arabic digits.
     */
    public static String toRoman(int number) {
        if (number < 1) {
            return Integer.toString(number);
        }
        StringBuilder roman = new StringBuilder();
        int rest = number;
        for (int i = 0; i < VALUES.length; i++) {
            while (rest >= VALUES[i]) {
                roman.append(SYMBOLS[i]);
                rest -= VALUES[i];
            }
        }
        return roman.toString();
    }
}
