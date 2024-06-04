package me.niko302.autoreplant;

import org.bukkit.enchantments.Enchantment;
import java.lang.reflect.Field;

public class EnchantmentUtils {

    private static final String LOOT_BONUS_BLOCKS_OLD = "LOOT_BONUS_BLOCKS";
    private static final String LOOTING_NEW = "FORTUNE";

    public static String getFortuneEnchantmentName() {
        try {
            // Attempt to access the LOOT_BONUS_BLOCKS field
            Field lootBonusBlocksField = Enchantment.class.getDeclaredField(LOOT_BONUS_BLOCKS_OLD);
            return LOOT_BONUS_BLOCKS_OLD;
        } catch (NoSuchFieldException e) {
            // If the field doesn't exist, return the new enchantment name
            return LOOTING_NEW;
        }
    }
}