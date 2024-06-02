package me.niko302.autoreplant;

import me.niko302.autoreplant.commands.Commands;
import me.niko302.autoreplant.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
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

        if (isFullyGrownCrop(block) && configManager.getAllowedItems().contains(toolMaterial)) {
            event.setCancelled(true);

            block.setBlockData(getInitialCropBlockData(blockType));

            for (ItemStack drop : getCropDrops(blockType, player.getInventory().getItemInMainHand(), configManager.useFortune())) {
                block.getWorld().dropItem(block.getLocation(), drop);
            }
        }
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
        int dropAmount = baseAmount + new Random().nextInt(fortuneLevel + 1);
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
        if (tool != null && tool.hasItemMeta() && tool.getItemMeta().hasEnchant(Enchantment.FORTUNE)) {
            return tool.getItemMeta().getEnchantLevel(Enchantment.FORTUNE);
        }
        return 0;
    }
}