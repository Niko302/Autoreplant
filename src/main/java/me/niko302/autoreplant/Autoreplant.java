package me.niko302.autoreplant;

import me.niko302.autoreplant.commands.Commands;
import me.niko302.autoreplant.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class Autoreplant extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private List<UUID> enabledPlayers;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        configManager = new ConfigManager(this);
        enabledPlayers = new ArrayList<>();
        getCommand("autoreplant").setExecutor(new Commands(this)); // Registering the command executor
        getLogger().info("Autoreplant has started successfully.");

        // Load autoreplant state from data.yml
        loadPlayerStates();
    }

    @Override
    public void onDisable() {
        // Save all enabled player states to data.yml
        savePlayerStates();

        getLogger().info("Autoreplant has stopped.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();
        Material toolMaterial = player.getInventory().getItemInMainHand().getType();

        if (!isAutoreplantEnabled(player) || !player.hasPermission("autoreplant.use")) {
            return;
        }

        // Check if the player has the permission to ignore tool restrictions
        boolean ignoreToolRestrictions = player.hasPermission("autoreplant.ignore.tool");

        // Check if the block is a crop block
        if (isCropBlock(block)) {
            // Check if the block is fully grown and either the tool is allowed or tool restrictions are ignored
            if (isFullyGrownCrop(block) && (configManager.getAllowedItems().contains(toolMaterial) || ignoreToolRestrictions)) {
                event.setCancelled(true);

                block.setBlockData(getInitialCropBlockData(blockType));

                for (ItemStack drop : getCropDrops(blockType, player.getInventory().getItemInMainHand(), configManager.useFortune())) {
                    block.getWorld().dropItem(block.getLocation(), drop);
                }
            } else {
                // If the crop is not fully grown, cancel the event and restore the block to its original state
                event.setCancelled(true);
                block.getState().update(true, false);
            }
        }
    }

    // Method to check if the block is a crop block
    private boolean isCropBlock(Block block) {
        return block.getType() == Material.WHEAT ||
                block.getType() == Material.CARROTS ||
                block.getType() == Material.POTATOES ||
                block.getType() == Material.BEETROOTS ||
                block.getType() == Material.COCOA ||
                block.getType() == Material.NETHER_WART;
    }


    public boolean isAutoreplantEnabled(Player player) {
        return enabledPlayers.contains(player.getUniqueId());
    }

    public void setAutoreplantEnabled(Player player, boolean enabled) {
        if (enabled) {
            enabledPlayers.add(player.getUniqueId());
        } else {
            enabledPlayers.remove(player.getUniqueId());
        }

        // Save the state to data.yml
        savePlayerState(player.getUniqueId(), enabled);
    }

    private void savePlayerState(UUID playerId, boolean enabled) {
        File dataFile = new File(getDataFolder(), "data.yml");
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        dataConfig.set(playerId.toString() + ".autoreplantEnabled", enabled);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePlayerStates() {
        File dataFile = new File(getDataFolder(), "data.yml");
        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        for (UUID playerId : enabledPlayers) {
            dataConfig.set(playerId.toString() + ".autoreplantEnabled", true);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlayerStates() {
        File dataFile = new File(getDataFolder(), "data.yml");
        if (dataFile.exists()) {
            YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            for (String playerId : dataConfig.getKeys(false)) {
                boolean enabled = dataConfig.getBoolean(playerId + ".autoreplantEnabled", false);
                if (enabled) {
                    enabledPlayers.add(UUID.fromString(playerId));
                }
            }
        }
    }

    private boolean isFullyGrownCrop(Block block) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return false;
    }

    private BlockData getInitialCropBlockData(Material cropType) {
        BlockData blockData = cropType.createBlockData();
        if (blockData instanceof Ageable) {
            ((Ageable) blockData).setAge(0);
        }
        return blockData;
    }

    private List<ItemStack> getCropDrops(Material cropType, ItemStack tool, boolean useFortune) {
        List<ItemStack> drops = new ArrayList<>();
        int baseAmount = getBaseAmount(cropType);
        int fortuneLevel = useFortune ? getFortuneLevel(tool) : 0; // Only consider Fortune if enabled in config
        int dropAmount = baseAmount;
        switch (cropType) {
            case WHEAT:
                dropAmount += new Random().nextInt(3) + 1;
                drops.add(new ItemStack(Material.WHEAT, dropAmount));
                drops.add(new ItemStack(Material.WHEAT_SEEDS, dropAmount));
                break;
            case CARROTS:
                dropAmount += new Random().nextInt(3) + 1;
                drops.add(new ItemStack(Material.CARROT, dropAmount));
                break;
            case POTATOES:
                dropAmount += new Random().nextInt(3) + 1;
                drops.add(new ItemStack(Material.POTATO, dropAmount));
                break;
            case BEETROOTS:
                dropAmount += new Random().nextInt(3) + 1;
                drops.add(new ItemStack(Material.BEETROOT, dropAmount));
                drops.add(new ItemStack(Material.BEETROOT_SEEDS, dropAmount));
                break;
            case COCOA:
                dropAmount += new Random().nextInt(2) + 2;
                drops.add(new ItemStack(Material.COCOA_BEANS, dropAmount));
                break;
            case NETHER_WART:
                dropAmount += new Random().nextInt(2) + 2;
                drops.add(new ItemStack(Material.NETHER_WART, dropAmount));
                break;
        }
        return drops;
    }


    private int getBaseAmount(Material cropType) {
        // Define base drop amounts for different crop types
        switch (cropType) {
            case WHEAT:
                return 1;
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
                return 3;
            case COCOA:
            case NETHER_WART:
                return 2;
            default:
                return 0;
        }
    }

    private int getFortuneLevel(ItemStack tool) {
        if (tool != null && tool.hasItemMeta()) {
            ItemMeta meta = tool.getItemMeta();
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                for (String line : lore) {
                    if (line.contains("Fortune")) {
                        String[] parts = line.split(" ");
                        for (String part : parts) {
                            if (part.matches("[IVXLCDM]+")) {
                                // Convert Roman numeral to integer
                                return romanToInteger(part);
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    // Method to convert Roman numerals to integer
    private int romanToInteger(String roman) {
        int result = 0;
        for (int i = 0; i < roman.length(); i++) {
            char currentChar = roman.charAt(i);
            int currentValue = romanCharToInt(currentChar);
            if (i + 1 < roman.length()) {
                int nextValue = romanCharToInt(roman.charAt(i + 1));
                if (currentValue < nextValue) {
                    result -= currentValue;
                } else {
                    result += currentValue;
                }
            } else {
                result += currentValue;
            }
        }
        return result;
    }

    // Method to convert Roman numerals to integer values
    private int romanCharToInt(char c) {
        switch (c) {
            case 'I':
                return 1;
            case 'V':
                return 5;
            case 'X':
                return 10;
            case 'L':
                return 50;
            case 'C':
                return 100;
            case 'D':
                return 500;
            case 'M':
                return 1000;
            default:
                return 0;
        }
    }
}
