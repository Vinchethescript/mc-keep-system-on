package it.vinche.keepsystemon.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

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
            StringUtil.copyPartialMatches(args[0], List.of("on", "off"), completions);
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("keep-system-on.inhibit")) {
            sender.sendMessage("§cYou do not have permission to execute this command.");
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("on")) {
                if (inhibitor.inhibit(plugin.getInhibitReason())) {
                    sender.sendMessage("Shutdown and sleep are now locked.");
                } else {
                    sender.sendMessage("Failed to enable inhibition. Check the server logs for details.");
                }
            } else if (args[0].equalsIgnoreCase("off")) {
                if (inhibitor.unhibit()) {
                    sender.sendMessage("Released inhibition. Shutdown and sleep are now allowed.");
                } else {
                    sender.sendMessage("Failed to disable inhibition. Check the server logs for details.");
                }
            }
        } else {
            if (inhibitor.isInhibited()) {
                sender.sendMessage("Inhibition is currently enabled. The system will not sleep or shutdown.");
            } else {
                sender.sendMessage("Inhibition is currently disabled.");
            }
        }
        return true;
    }
}
