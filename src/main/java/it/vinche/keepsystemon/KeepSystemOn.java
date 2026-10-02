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


public class KeepSystemOn extends JavaPlugin implements Listener {
    public enum InhibitWhen {
        ALWAYS,
        ACTIVE,
        NEVER;
    }

    FileConfiguration config = getConfig();
    public InhibitWhen inhibitMode;

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
            if (doInhibit()) {
                getLogger().info("Plugin enabled and inhibiting sleep/shutdown now.");
            }
        } else if (inhibitMode == InhibitWhen.ACTIVE) {
            getLogger().info("Plugin enabled and inhibiting sleep/shutdown when players are online.");
            autoInhibit();
        } else {
            getLogger().info("Plugin enabled and inhibiting sleep/shutdown when /inhibit is used.");
        }
        getServer().getPluginManager().registerEvents(this, this);
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

    public boolean doInhibit() {
        try {
            inhibitor.inhibit(getInhibitReason());
        } catch (Exception e) {
            getLogger().severe("Failed to enable inhibition: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean unInhibit() {
        try {
            inhibitor.unhibit();
        } catch (Exception e) {
            getLogger().severe("Failed to disable inhibition: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled, releasing sleep/shutdown inhibition.");
        try {
            inhibitor.unhibit();
        } catch (Exception e) {
            getLogger().severe("Failed to disable inhibition: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean autoInhibit() {
        if (inhibitMode == InhibitWhen.ACTIVE) {
            if (getServer().getOnlinePlayers().size() > 0) {
                // this will either acquire the inhibition or change the reason for the player count 
                return doInhibit();
            } else {
                return unInhibit();
            }
        }
        return true;
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