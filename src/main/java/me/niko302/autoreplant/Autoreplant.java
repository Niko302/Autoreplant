package me.niko302.autoreplant;

import me.niko302.autoreplant.commands.Commands;
import me.niko302.autoreplant.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
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
    private String fortuneEnchantmentName;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        configManager = new ConfigManager(this);
        enabledPlayers = new ArrayList<>();
        getCommand("autoreplant").setExecutor(new Commands(this)); // Registering the command executor
        getLogger().info("Autoreplant has started successfully.");

        // Initialize the fortuneEnchantmentName field
        fortuneEnchantmentName = EnchantmentUtils.getFortuneEnchantmentName();

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
        // Check if the event was cancelled by another plugin
        if (event.isCancelled()) {
//            getLogger().info("Block break event was cancelled by another plugin.");
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Add logging to check the player's tool
//        getLogger().info("Player's tool: " + tool.toString());

        if (!isAutoreplantEnabled(player) || !player.hasPermission("autoreplant.use")) {
            return;
        }

        // Check if the block is a crop block
        if (isCropBlock(block)) {
            // Check if the correct tool is used, or if the player has the ignore tool restrictions permission
            if (!configManager.getAllowedItems().contains(tool.getType()) && !player.hasPermission("autoreplant.ignoretoolrestrictions")) {
                // If the wrong tool is used, let the block break naturally
                // No need to cancel the event here
                block.breakNaturally();
                return;
            }

            // Check if the block is fully grown
            if (isFullyGrownCrop(block)) {
                event.setCancelled(true);

                // Capture the direction of the cocoa block before breaking it
                BlockFace cocoaFace = null;
                if (blockType == Material.COCOA) {
                    cocoaFace = ((Directional) block.getBlockData()).getFacing();
                }

                // Get the initial crop block data and reapply direction if it's cocoa
                BlockData newBlockData = getInitialCropBlockData(blockType);
                if (blockType == Material.COCOA && cocoaFace != null) {
                    ((Directional) newBlockData).setFacing(cocoaFace);
                }

                block.setBlockData(newBlockData);

                for (ItemStack drop : getCropDrops(blockType, tool, configManager.useFortune())) {
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
//        getLogger().info("Base Amount: " + baseAmount);
        int fortuneLevel = useFortune ? getFortuneLevel(tool) : 0; // Only consider Fortune if enabled in config
//        getLogger().info("Fortune Level: " + fortuneLevel);
        int dropAmount = baseAmount;

        // Adjust drop amount based on Fortune level
        dropAmount += fortuneLevel;
//        getLogger().info("Drop Amount: " + dropAmount);

        switch (cropType) {
            case WHEAT:
                drops.add(new ItemStack(Material.WHEAT, dropAmount));
                drops.add(new ItemStack(Material.WHEAT_SEEDS, dropAmount));
                break;
            case CARROTS:
                drops.add(new ItemStack(Material.CARROT, dropAmount));
                break;
            case POTATOES:
                drops.add(new ItemStack(Material.POTATO, dropAmount));
                break;
            case BEETROOTS:
                drops.add(new ItemStack(Material.BEETROOT, dropAmount));
                drops.add(new ItemStack(Material.BEETROOT_SEEDS, dropAmount));
                break;
            case COCOA:
                drops.add(new ItemStack(Material.COCOA_BEANS, dropAmount));
                break;
            case NETHER_WART:
                drops.add(new ItemStack(Material.NETHER_WART, dropAmount));
                break;
        }

//        getLogger().info("Drops: " + drops.toString());
        return drops;
    }



    private int getBaseAmount(Material cropType) {
        // Define base drop amounts for different crop types
        switch (cropType) {
            case WHEAT:
                return 1;
            case CARROTS:
                return 1 + new Random().nextInt(4); // Random between 1 and 4
            case POTATOES:
                return 1 + new Random().nextInt(4); // Random between 1 and 4
            case BEETROOTS:
                return 1 + new Random().nextInt(2); // Random between 1 and 2
            case COCOA:
                return 1 + new Random().nextInt(3); // Random between 1 and 3
            case NETHER_WART:
                return 2 + new Random().nextInt(3); // Random between 2 and 4
            default:
                return 0;
        }
    }

    private int getFortuneLevel(ItemStack tool) {
        if (tool != null && tool.hasItemMeta()) {
            ItemMeta meta = tool.getItemMeta();
            if (meta.hasEnchants()) {
                if (fortuneEnchantmentName != null) {
                    if (meta.getEnchants().containsKey(Enchantment.getByName(fortuneEnchantmentName))) {
                        return meta.getEnchantLevel(Enchantment.getByName(fortuneEnchantmentName));
                    }
                }
            }
        }
        return 0;
    }

}
