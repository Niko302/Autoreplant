// Commands.java
package me.niko302.autoreplant.commands;
import me.niko302.autoreplant.Autoreplant;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
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
            TextComponent noPermissionMessage = new TextComponent("You don't have permission to use this command!");
            noPermissionMessage.setColor(ChatColor.of(new java.awt.Color(139, 0, 0))); // Dark red color
            player.spigot().sendMessage(noPermissionMessage);
            return true;
        }

        if (args.length == 0) {
            boolean newStatus = !plugin.isAutoreplantEnabled(player);
            plugin.setAutoreplantEnabled(player, newStatus);
            TextComponent statusMessage = new TextComponent("Autoreplant is now ");
            if (newStatus) {
                TextComponent enabledMessage = new TextComponent("enabled");
                enabledMessage.setColor(ChatColor.of(new java.awt.Color(0, 255, 0))); // Green color
                statusMessage.addExtra(enabledMessage);
            } else {
                TextComponent disabledMessage = new TextComponent("disabled");
                disabledMessage.setColor(ChatColor.of(new java.awt.Color(255, 0, 0))); // Red color
                statusMessage.addExtra(disabledMessage);
            }
            player.spigot().sendMessage(statusMessage);
        } else {
            TextComponent usageMessage = new TextComponent("Usage: /autoreplant");
            player.spigot().sendMessage(usageMessage);
        }

        return true;
    }
}
