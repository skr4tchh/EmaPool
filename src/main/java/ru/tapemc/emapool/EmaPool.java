package ru.tapemc.emapool;

import net.raidstone.wgevents.WorldGuardEvents;
import net.raidstone.wgevents.events.RegionEnteredEvent;
import net.raidstone.wgevents.events.RegionLeftEvent;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EmaPool extends JavaPlugin implements Listener {

    private Map<UUID, Long> playersOnLake = new HashMap<>();

    private String WORLD;

    private String REGION;

    private int RUBLE_REWARD;

    private int PERIOD;

    private int STAY_TIME_REQUIRED;

    @Override
    public void onEnable() {
        saveResource("config.yml", false);
        Bukkit.getPluginManager().registerEvents(this, this);
        this.WORLD = getConfig().getString("world");
        this.REGION = getConfig().getString("region");
        this.STAY_TIME_REQUIRED = getConfig().getInt("time");
        this.RUBLE_REWARD = getConfig().getInt("ruble_reward");
        this.PERIOD = getConfig().getInt("period") * 20;

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Map.Entry<UUID, Long> entry : playersOnLake.entrySet()) {
                UUID uuid = entry.getKey();
                long timestamp = entry.getValue();
                long stayTime = (System.currentTimeMillis() - timestamp) / 1000;

                double percentage = (double) stayTime / STAY_TIME_REQUIRED * 100;
                String formattedPercentage = "§c" + (int) percentage + "%";

                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    StringBuilder progressBar = new StringBuilder("§8[");
                    int progressBarFilled = (int) (percentage / 10);
                    for(int i = 0; i < progressBarFilled; i++) {
                        progressBar.append("§a|");
                    }
                    for(int i = 0; i < 10 - progressBarFilled; i++) {
                        progressBar.append("§7|");
                    }
                    progressBar.append("§8]");
                    player.sendTitle(progressBar.toString(), formattedPercentage, 0, 130, 0);
                }

                if (stayTime >= STAY_TIME_REQUIRED) {
                    PlayerPoints.getInstance().getAPI().give(uuid, RUBLE_REWARD);
                    playersOnLake.put(player.getUniqueId(), System.currentTimeMillis());
                    player.clearTitle();
                }

            }
        }, 0, PERIOD);
    }

    @EventHandler
    public void onEntered(RegionEnteredEvent event) {
        Player player = event.getPlayer();
        if(player.getWorld().getName().equalsIgnoreCase(WORLD) && event.getRegionName().equalsIgnoreCase(REGION)) {
            playersOnLake.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendTitle("§fВы вошли в §6рублевое озеро", "");
        }
    }

    @EventHandler
    public void onLeft(RegionLeftEvent event) {
        Player player = event.getPlayer();
        if(player.getWorld().getName().equalsIgnoreCase(WORLD) && event.getRegionName().equalsIgnoreCase(REGION)) {
            playersOnLake.remove(player.getUniqueId());
            player.sendTitle("§fВы покинули §6рублевое озеро", "");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if(player.getWorld().getName().equalsIgnoreCase(WORLD) && WorldGuardEvents.isPlayerInAllRegions(uuid, REGION)) {
            playersOnLake.put(uuid, System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if(playersOnLake.containsKey(uuid)) {
            playersOnLake.remove(uuid);
        }
    }

}
