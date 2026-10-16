package cz.eida.minecraft.timebar.calendar;

/**
 * One reading of a world's calendar for the bossbar. The calendar may be custom: any number of
 * months with any names and lengths and any week, so nothing here assumes the Gregorian one.
 *
 * @param seasonKey  season identity: WINTER, SPRING, SUMMER, FALL or DISABLED
 * @param seasonName season display name, may contain legacy color codes
 * @param day        day of the month from 1
 * @param month      month of the year from 1 (its order in the calendar)
 * @param monthCount months in the calendar's year, 0 when the plugin does not tell
 * @param monthName  month name as the calendar has it
 * @param year       year of the date
 * @param weekday    day of the week as the calendar has it
 * @param worldYear  year for {WORLDYEAR} and {WORLDYEAR-ROMAN}
 * @param dayCount   days the world has lived
 * @param hours      hour of the day 0-23
 * @param minutes    minute 0-59
 * @param seconds    second 0-59
 */
public record CalendarDay(String seasonKey, String seasonName, int day, int month, int monthCount, String monthName,
                          int year, String weekday, int worldYear, long dayCount, int hours, int minutes, int seconds) {

    /** clock of a world tick: tick 0 is 6:00, 6000 noon, 18000 midnight */
    public static int[] clock(long worldTime) {
        long ticks = Math.floorMod(worldTime, 24000L);
        int hours = (int) ((ticks / 1000 + 6) % 24);
        int minutes = (int) (ticks % 1000 * 60 / 1000);
        int seconds = (int) (ticks % 1000 * 3600 / 1000 % 60);
        return new int[]{hours, minutes, seconds};
    }

    /** part of the day gone since midnight, 0-1 */
    public float dayProgress() {
        return (float) ((hours * 3600 + minutes * 60 + seconds) / 86400.0);
    }
}
