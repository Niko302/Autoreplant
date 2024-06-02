package me.niko302.autoreplant.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    private boolean shouldReplant;
    private boolean useFortune; // New variable to store the use-fortune setting

    // Define default materials here
    private final List<Material> defaultAllowedItems = Arrays.asList(
            Material.WOODEN_PICKAXE,
            Material.STONE_PICKAXE,
            Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE,
            Material.DIAMOND_HOE
    );

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Check if allowed-items list is empty, then populate it with defaults
        if (config.getStringList("allowed-items").isEmpty()) {
            List<String> defaultItemNames = new ArrayList<>();
            for (Material material : defaultAllowedItems) {
                defaultItemNames.add(material.name());
            }
            config.set("allowed-items", defaultItemNames);
            plugin.saveConfig();
        }

        // Load use-fortune setting
        useFortune = config.getBoolean("use-fortune", true);
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
}
