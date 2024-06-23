package me.niko302.autoreplant;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import com.jeff_media.updatechecker.UserAgentBuilder;
import lombok.Getter;
import me.niko302.autoreplant.commands.AutoReplantCommand;
import me.niko302.autoreplant.config.ConfigManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class AutoReplant extends JavaPlugin implements Listener {

    private final List<UUID> enabledPlayers = new ArrayList<>();

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        getCommand("autoreplant").setExecutor(new AutoReplantCommand(this)); // Registering the command executor
        getServer().getPluginManager().registerEvents(this, this);

        new Metrics(this, 22206);

        // Load autoreplant state from data.yml
        loadPlayerStates();

        new UpdateChecker(this, UpdateCheckSource.SPIGOT, "117106")
                .setNotifyRequesters(false)
                .setNotifyOpsOnJoin(false)
                .setUserAgent(UserAgentBuilder.getDefaultUserAgent())
                .checkEveryXHours(12)
                .onSuccess((commandSenders, latestVersion) -> {
                    String messagePrefix = "&8[&6Auto Replant&8] ";
                    String currentVersion = getDescription().getVersion();

                    if (currentVersion.equalsIgnoreCase(latestVersion)) {
                        String updateMessage = color(messagePrefix + "&aYou are using the latest version of AutoReplant!");

                        Bukkit.getConsoleSender().sendMessage(updateMessage);
                        Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).forEach(player -> player.sendMessage(updateMessage));
                        return;
                    }

                    List<String> updateMessages = List.of(
                            color(messagePrefix + "&cYour version of AutoReplant is outdated!"),
                            color(String.format(messagePrefix + "&cYou are using %s, latest is %s!", currentVersion, latestVersion)),
                            color(messagePrefix + "&cDownload latest here:"),
                            color("&6https://www.spigotmc.org/resources/autoreplant-1-20.117106/")
                    );

                    Bukkit.getConsoleSender().sendMessage(updateMessages.toArray(new String[]{}));
                    Bukkit.getOnlinePlayers().stream().filter(ServerOperator::isOp).forEach(player -> player.sendMessage(updateMessages.toArray(new String[]{})));
                })
                .onFail((commandSenders, e) -> {}).checkNow();
    }

    @Override
    public void onDisable() {

    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if the event was cancelled by another plugin
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!enabledPlayers.contains(player.getUniqueId()) || !player.hasPermission("autoreplant.use")) {
            return;
        }

        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }

        // Check if the correct tool is used, or if the player has the ignore tool restrictions permission
        if (!configManager.getAllowedItems().contains(tool.getType()) && !player.hasPermission("autoreplant.ignoretoolrestrictions")) {
            return;
        }

        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        // Capture the direction of the cocoa block before breaking it

        // Schedule the replanting task
        getServer().getScheduler().runTaskLater(this, () -> {
            block.setType(blockType);

            ageable.setAge(0);
            block.setBlockData(ageable);
        }, 2); // Run 1 tick later to ensure the block is set correctly
    }

    public void savePlayerState(UUID playerId, boolean enabled) {
        File dataFile = new File(getDataFolder(), "data.yml");
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        dataConfig.set(playerId.toString() + ".autoreplantEnabled", enabled);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlayerStates() {
        File dataFile = new File(getDataFolder(), "data.yml");

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String playerId : dataConfig.getKeys(false)) {
            boolean enabled = dataConfig.getBoolean(playerId + ".autoreplantEnabled", false);

            if (!enabled) {
                continue;
            }

            enabledPlayers.add(UUID.fromString(playerId));
        }
    }

}