package me.niko302.autoreplant.commands;

import me.niko302.autoreplant.Autoreplant;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.Color;

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
            TextComponent noPermissionMessage = parseMessage(plugin.getConfig().getString("messages.no_permission"));
            player.spigot().sendMessage(noPermissionMessage);
            return true;
        }

        if (args.length == 0) {
            boolean newStatus = !plugin.isAutoreplantEnabled(player);
            plugin.setAutoreplantEnabled(player, newStatus);
            String statusMessageText = plugin.getConfig().getString(newStatus ? "messages.enabled" : "messages.disabled");
            TextComponent statusMessage = parseMessage(statusMessageText);
            player.spigot().sendMessage(statusMessage);
        } else {
            TextComponent usageMessage = parseMessage(plugin.getConfig().getString("messages.usage"));
            player.spigot().sendMessage(usageMessage);
        }

        return true;
    }

    private TextComponent parseMessage(String message) {
        TextComponent textComponent = new TextComponent();
        StringBuilder currentText = new StringBuilder();
        ChatColor currentColor = ChatColor.WHITE;

        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '&' && i + 7 < message.length()) {
                // Flush the current text with the current color
                if (currentText.length() > 0) {
                    TextComponent part = new TextComponent(currentText.toString());
                    part.setColor(currentColor);
                    textComponent.addExtra(part);
                    currentText.setLength(0);
                }

                // Parse the new color
                String colorCode = message.substring(i + 1, i + 7);
                try {
                    Color color = Color.decode("#" + colorCode);
                    currentColor = ChatColor.of(color);
                } catch (NumberFormatException e) {
                    // Invalid color code, continue with the previous color
                }
                i += 6; // Skip the color code
            } else {
                currentText.append(message.charAt(i));
            }
        }

        // Add the remaining text
        if (currentText.length() > 0) {
            TextComponent part = new TextComponent(currentText.toString());
            part.setColor(currentColor);
            textComponent.addExtra(part);
        }

        return textComponent;
    }
}