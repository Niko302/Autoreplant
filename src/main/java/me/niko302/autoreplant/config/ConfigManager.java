package me.niko302.autoreplant.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    private boolean useFortune; // Variable to store the use-fortune setting
    private boolean autoreplantEnabled; // Variable to store the autoreplant status

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load use-fortune setting
        useFortune = config.getBoolean("use-fortune", true);

        // Load autoreplant setting
        autoreplantEnabled = config.getBoolean("autoreplant-enabled", true);
    }

    public List<Material> getAllowedItems() {
        List<Material> allowedItems = new ArrayList<>();
        List<String> itemNames = config.getStringList("allowed-items");
        for (String itemName : itemNames) {
            try {
                Material material = Material.valueOf(itemName);
                allowedItems.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material name '" + itemName + "' in config.yml");
            }
        }
        return allowedItems;
    }

    public boolean useFortune() {
        return useFortune;
    }

    public boolean isAutoreplantEnabled() {
        return autoreplantEnabled;
    }
}