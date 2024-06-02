package me.niko302.autoreplant;

import me.niko302.autoreplant.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Autoreplant extends JavaPlugin implements Listener {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
        configManager = new ConfigManager(this);
        System.out.println("Autoreplant has started successfully.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("Autoreplant has stopped.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material blockType = block.getType();
        Material toolMaterial = event.getPlayer().getInventory().getItemInMainHand().getType();

        if (isFullyGrownCrop(block) && configManager.getAllowedItems().contains(toolMaterial)) {
            event.setCancelled(true); // Prevent the block from being broken

            // Replant the crop
            block.setBlockData(getInitialCropBlockData(blockType));

            // Drop the harvested items
            for (ItemStack drop : getCropDrops(blockType, event.getPlayer().getInventory().getItemInMainHand(), configManager.useFortune())) {
                block.getWorld().dropItem(block.getLocation(), drop);
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
