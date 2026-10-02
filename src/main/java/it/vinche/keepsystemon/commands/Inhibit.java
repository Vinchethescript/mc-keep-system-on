package it.vinche.keepsystemon.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.bukkit.entity.Player;

import it.vinche.keepsystemon.KeepSystemOn;
import it.vinche.keepsystemon.inhibitors.Inhibitor;

public class Inhibit implements TabExecutor {

    private Inhibitor inhibitor;
    private KeepSystemOn plugin;

    public Inhibit(Inhibitor inhibitor, KeepSystemOn plugin) {
        this.inhibitor = inhibitor;
        this.plugin = plugin;
    }

    @Override 
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], List.of("on", "off", "auto"), completions);
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("keep-system-on.inhibit")) {
            sender.sendMessage("§cYou do not have permission to execute this command.");
            return true;
        }

        try {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("on")) {
                    inhibitor.inhibit(plugin.getInhibitReason());
                    plugin.inhibitMode = KeepSystemOn.InhibitWhen.ALWAYS;
                    sender.sendMessage("Shutdown and sleep are now locked.");
                } else if (args[0].equalsIgnoreCase("off")) {
                    inhibitor.unhibit();
                    plugin.inhibitMode = KeepSystemOn.InhibitWhen.NEVER;
                    sender.sendMessage("Released inhibition. Shutdown and sleep are now allowed.");
                } else if (args[0].equalsIgnoreCase("auto")) {
                    plugin.inhibitMode = KeepSystemOn.InhibitWhen.ACTIVE;
                    plugin.autoInhibit();
                    sender.sendMessage("Inhibition will now be automatically enabled when players are online.");
                } else {
                    return false;
                }
            } else {
                if (inhibitor.isInhibited()) {
                    sender.sendMessage("Inhibition is currently enabled. The system will not sleep or shutdown.");
                } else {
                    sender.sendMessage("Inhibition is currently disabled.");
                }
            }
        } catch (Exception e) {
            if (sender instanceof Player) {
                sender.sendMessage("§cAn error occurred while executing the command. Check the server logs for details.");
            }
        }
        return true;
    }
}
