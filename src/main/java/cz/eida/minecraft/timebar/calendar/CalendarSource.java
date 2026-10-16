package cz.eida.minecraft.timebar.calendar;

import org.bukkit.World;
import org.bukkit.plugin.PluginManager;

import java.util.Optional;

/** Plugin with a world calendar and seasons that the bossbar reads. */
public interface CalendarSource {

    /** plugin name for the log */
    String name();

    /** the plugin's API is reachable */
    boolean available();

    /** today of the world; empty when the world has no calendar */
    Optional<CalendarDay> read(World world);

    /** what an admin does when a world has no calendar */
    String setupHint();

    /**
     * OpenSeasons first: with {@code rs.compatibility} it also answers as RealisticSeasons, but its
     * own API knows the whole calendar. Classes of the other plugin are loaded only when it is present.
     */
    static Optional<CalendarSource> detect(PluginManager plugins) {
        if (plugins.getPlugin("OpenSeasons") != null) {
            return Optional.of(new OpenSeasonsSource());
        }
        if (plugins.getPlugin("RealisticSeasons") != null) {
            return Optional.of(new RealisticSeasonsSource());
        }
        return Optional.empty();
    }
}
