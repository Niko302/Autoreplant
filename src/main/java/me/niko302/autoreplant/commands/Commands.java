// Commands.java
package me.niko302.autoreplant.commands;

import me.niko302.autoreplant.Autoreplant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    private final Autoreplant plugin;

    public Commands(Autoreplant plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("autoreplant.use")) {
            player.sendMessage("You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            boolean newStatus = !plugin.isAutoreplantEnabled(player);
            plugin.setAutoreplantEnabled(player, newStatus);
            player.sendMessage("Autoreplant is now " + (newStatus ? "enabled" : "disabled"));
        } else {
            player.sendMessage("Usage: /autoreplant");
        }

        return true;
    }
}