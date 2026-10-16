package cz.eida.minecraft.timebar.calendar;

import me.casperge.realisticseasons.api.SeasonsAPI;
import me.casperge.realisticseasons.calendar.Date;
import me.casperge.realisticseasons.season.Season;
import org.bukkit.World;

import java.util.Optional;

/**
 * RealisticSeasons API. Month and weekday names come from its calendar; {@code Season.toString()}
 * is the display name. The year for {WORLDYEAR} stays the world's age in 365-day years, as before.
 */
final class RealisticSeasonsSource implements CalendarSource {

    private final SeasonsAPI api = SeasonsAPI.getInstance();

    @Override
    public String name() {
        return "RealisticSeasons";
    }

    @Override
    public boolean available() {
        return api != null;
    }

    @Override
    public Optional<CalendarDay> read(World world) {
        Date date = api.getDate(world);
        // this should be null if RealisticSeasons hasn't been set up yet
        if (date == null) {
            return Optional.empty();
        }
        Season season = api.getSeason(world);
        return Optional.of(new CalendarDay(season.name(), season.toString(), date.getDay(), date.getMonth(), 0,
                api.getCurrentMonthName(world), date.getYear(), api.getDayOfWeek(world),
                (int) (world.getFullTime() / 24000 / 365), world.getFullTime() / 24000,
                api.getHours(world), api.getMinutes(world), api.getSeconds(world)));
    }

    @Override
    public String setupHint() {
        return "Enter the world and type '/rs set <season>'";
    }
}
