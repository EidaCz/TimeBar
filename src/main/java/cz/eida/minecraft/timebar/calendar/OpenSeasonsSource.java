package cz.eida.minecraft.timebar.calendar;

import cz.eida.minecraft.openseasons.api.OpenSeasonsApi;
import cz.eida.minecraft.openseasons.calendar.CalendarDate;
import cz.eida.minecraft.openseasons.calendar.GameCalendar;
import cz.eida.minecraft.openseasons.season.Season;
import org.bukkit.World;

import java.util.Optional;

/**
 * OpenSeasons API (MC-215): custom calendar with any months and week, display names from its
 * names.yml / lang.yml. The year for {WORLDYEAR} is the calendar year. The API is looked up on every
 * read, so a reload of OpenSeasons does not leave a stale instance here.
 */
final class OpenSeasonsSource implements CalendarSource {

    @Override
    public String name() {
        return "OpenSeasons";
    }

    @Override
    public boolean available() {
        return OpenSeasonsApi.get().isPresent();
    }

    @Override
    public Optional<CalendarDay> read(World world) {
        Optional<OpenSeasonsApi> found = OpenSeasonsApi.get();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        OpenSeasonsApi api = found.get();
        Optional<CalendarDate> today = api.date(world);
        if (today.isEmpty()) {
            return Optional.empty();
        }
        CalendarDate date = today.get();
        GameCalendar calendar = api.calendar();
        Optional<Season> season = api.season(world);
        int[] clock = CalendarDay.clock(world.getTime());
        return Optional.of(new CalendarDay(season.map(Season::name).orElse("DISABLED"),
                season.map(api::seasonName).orElse("DISABLED"), date.day(), date.month(), calendar.months().size(),
                calendar.monthName(date), date.year(), calendar.weekdayName(date), date.year(),
                world.getFullTime() / 24000, clock[0], clock[1], clock[2]));
    }

    @Override
    public String setupHint() {
        return "Enter the world and type '/rs set <season>' (OpenSeasons)";
    }
}
