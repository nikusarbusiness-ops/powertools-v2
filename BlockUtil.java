package de.powertools;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BlockUtil {

    /** Spieler, deren Zusatzbloecke gerade gebrochen werden (verhindert Endlosschleifen). */
    public static final Set<UUID> BUSY = new HashSet<>();

    private BlockUtil() {
    }

    /** Bricht einen Block "als" der Spieler (Schutz-Plugins koennen das per Event verhindern). */
    public static boolean breakAs(Player player, Block block, ItemStack tool) {
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        if (event.isDropItems()) {
            block.breakNaturally(tool);
        } else {
            block.setType(Material.AIR);
        }
        int exp = event.getExpToDrop();
        if (exp > 0) {
            ExperienceOrb orb = block.getWorld().spawn(block.getLocation().add(0.5, 0.5, 0.5), ExperienceOrb.class);
            orb.setExperience(exp);
        }
        return true;
    }
}
