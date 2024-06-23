package me.niko302.autoreplant.commands;

import lombok.AccessLevel;
import lombok.Getter;
import me.niko302.autoreplant.AutoReplant;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoReplantCommand implements CommandExecutor {

    private final AutoReplant plugin;

    @Getter(AccessLevel.NONE)
    private final Pattern hexColorExtractor = Pattern.compile("#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})");

    public AutoReplantCommand(AutoReplant plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (!player.hasPermission("autoreplant.use")) {
            player.sendMessage(color(plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        if (args.length != 0) {
            player.sendMessage(color(plugin.getConfig().getString("messages.usage")));
            return true;
        }

        boolean newStatus = !plugin.getEnabledPlayers().contains(player.getUniqueId());

        if (newStatus) {
            plugin.getEnabledPlayers().add(player.getUniqueId());
        } else {
            plugin.getEnabledPlayers().remove(player.getUniqueId());
        }

        plugin.savePlayerState(player.getUniqueId(), newStatus);
        String statusMessageText = plugin.getConfig().getString(newStatus ? "messages.enabled" : "messages.disabled");
        player.sendMessage(color(statusMessageText));
        return true;
    }

    private String color(String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        Matcher matcher = hexColorExtractor.matcher(coloredMessage);

        while (matcher.find()) {
            String hexColor = matcher.group();
            coloredMessage = coloredMessage.replace(hexColor, ChatColor.of(hexColor).toString());
        }

        return coloredMessage;
    }

}