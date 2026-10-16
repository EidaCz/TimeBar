package cz.eida.minecraft.timebar.seasons;

import cz.eida.minecraft.timebar.calendar.CalendarDay;
import cz.eida.minecraft.timebar.calendar.CalendarSource;
import lol.hyper.timebar.TimeBar;
import lol.hyper.timebar.papi.PlaceholderUtil;
import lol.hyper.timebar.tracker.WorldTimeTracker;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Optional;

/**
 * Bossbar of a world with seasons (RealisticSeasons or OpenSeasons). Replaces the original
 * RealisticSeasonsTask, which read the date as a Gregorian {@code LocalDate}.
 */
public final class SeasonsTimeBarTask extends BukkitRunnable {

    private final WorldTimeTracker worldTimeTracker;
    private final CalendarSource source;
    private final World world;
    private final SeasonsTitle seasonsTitle;
    private boolean logged;

    public SeasonsTimeBarTask(WorldTimeTracker worldTimeTracker, CalendarSource source) {
        this.worldTimeTracker = worldTimeTracker;
        this.source = source;
        this.world = worldTimeTracker.getMainWorld();
        this.seasonsTitle = new SeasonsTitle(worldTimeTracker.timeBar.realisticSeasonsConfig, worldTimeTracker.timeBar.logger);
    }

    @Override
    public void run() {
        TimeBar timeBar = worldTimeTracker.timeBar;
        if (!source.available()) {
            timeBar.logger.severe("Unable to hook into " + source.name() + "!");
            timeBar.logger.severe("Canceling the TimeBar task, please try /timebar reload.");
            this.cancel();
            return;
        }
        Optional<CalendarDay> today = source.read(world);
        if (today.isEmpty()) {
            timeBar.logger.severe("Cannot retrieve date from " + source.name() + " for world " + world.getName());
            timeBar.logger.severe("Most likely means you did not setup " + source.name() + " for that defined world.");
            timeBar.logger.severe(source.setupHint());
            timeBar.logger.severe("After you setup the season, you can run '/timebar reload'");
            this.cancel();
            return;
        }
        CalendarDay day = today.get();
        String timeOfDay = seasonsTitle.timeOfDay(day);
        float progress = day.dayProgress();
        String title = seasonsTitle.title(day, timeOfDay);

        // store these into the tracker
        worldTimeTracker.setTimeOfDay(timeOfDay);
        worldTimeTracker.setDayCount((int) day.dayCount());
        worldTimeTracker.setDayPercent(progress * 100);

        BossBar.Color color = seasonsTitle.color(day, timeBar.bossBarColor);
        if (!logged) {
            // first title after start or reload, so the bar can be checked from the console
            logged = true;
            timeBar.logger.info("TimeBar for '" + world.getName() + "' (" + source.name() + ", " + day.day() + "/"
                    + day.month() + "/" + day.year() + "): "
                    + PlainTextComponentSerializer.plainText().serialize(timeBar.miniMessage.deserialize(title)));
        }

        // loop through all bossbars and format the title
        for (Map.Entry<Player, BossBar> entry : worldTimeTracker.getBossBars().entrySet()) {
            Player player = entry.getKey();
            BossBar bossBar = entry.getValue();
            // format if PAPI is detected
            String formattedTitle = timeBar.papiSupport ? PlaceholderUtil.format(player, title) : title;
            bossBar.name(timeBar.miniMessage.deserialize(formattedTitle));
            bossBar.progress(progress);
            bossBar.color(color);

            // if the player is holding a clock
            if (timeBar.config.getBoolean("hold-clock-to-show")) {
                if (player.getInventory().getItemInMainHand().getType() == Material.CLOCK) {
                    worldTimeTracker.showPlayer(player);
                } else {
                    worldTimeTracker.hidePlayer(player);
                }
            }
        }
    }
}
