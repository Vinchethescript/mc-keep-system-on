package it.vinche.keepsystemon;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import it.vinche.keepsystemon.commands.Inhibit;
import it.vinche.keepsystemon.inhibitors.Inhibitor;
import it.vinche.keepsystemon.inhibitors.SystemdInhibitor;

enum InhibitWhen {
    ALWAYS,
    ACTIVE,
    NEVER;
}

public class KeepSystemOn extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();
    InhibitWhen inhibitMode;

    private Inhibitor inhibitor;

    @Override
    public void onEnable() {
        config.addDefault("inhibit", "when-active");
        config.addDefault("inhibit-reason", "Minecraft server is running!");
        config.addDefault("player-count", "Players online: {online}/{max}");
        config.options().copyDefaults(true);
        saveConfig();

        inhibitor = new SystemdInhibitor();  // TODO: make this configurable, for now we just use systemd
        this.getCommand("inhibit").setExecutor(new Inhibit(inhibitor, this));
        
        if (config.getString("inhibit").equalsIgnoreCase("when-active")) {
            inhibitMode = InhibitWhen.ACTIVE;
        } else if (config.getBoolean("inhibit")) {
            inhibitMode = InhibitWhen.ALWAYS;
        } else {
            inhibitMode = InhibitWhen.NEVER;
        }

        if (inhibitMode == InhibitWhen.ALWAYS) {
            getLogger().info("Plugin enabled and inhibiting sleep/shutdown now.");
            inhibitor.inhibit(getInhibitReason());
        } else if (inhibitMode == InhibitWhen.ACTIVE) {
            getLogger().info("Plugin enabled and inhibiting sleep/shutdown when players are online.");
            getServer().getPluginManager().registerEvents(this, this);
            autoInhibit();
        } else {
            getLogger().info("Plugin enabled and inhibiting sleep/shutdown when /inhibit is used.");
        }
    }

    public String getInhibitReason() {
        String ret = config.getString("inhibit-reason");
        if (inhibitMode == InhibitWhen.ACTIVE) {
            ret += " " + config.getString("player-count")
                    .replace("{online}", String.valueOf(getServer().getOnlinePlayers().size()))
                    .replace("{max}", String.valueOf(getServer().getMaxPlayers()));
        }
        return ret;
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled, releasing sleep/shutdown inhibition.");
        if (inhibitor.isInhibited()) {
            inhibitor.unhibit();
        }
    }

    private void autoInhibit() {
        if (inhibitMode == InhibitWhen.ACTIVE) {
            if (getServer().getOnlinePlayers().size() > 0) {
                // this will either acquire the inhibition or change the reason for the player count 
                inhibitor.inhibit(getInhibitReason());
            } else {
                if (inhibitor.isInhibited()) {
                    inhibitor.unhibit();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        autoInhibit();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        getServer().getScheduler().runTask(this, this::autoInhibit);
    }
}