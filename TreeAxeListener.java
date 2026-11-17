package de.powertools;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/** Blitz Axe: faellt ganze Baeume. Schleichen (Shift) = nur ein Stamm. */
public class TreeAxeListener implements Listener {

    private static final BlockFace[] FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final PowerTools plugin;

    public TreeAxeListener(PowerTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (BlockUtil.BUSY.contains(player.getUniqueId())) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (plugin.items().getType(tool) != ItemManager.Type.BLITZ_AXE) {
            return;
        }
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR || player.isSneaking()) {
            return;
        }
        Block start = event.getBlock();
        if (!Tag.LOGS.isTagged(start.getType())) {
            return;
        }

        int max = Math.max(1, plugin.getConfig().getInt("blitz-axe.max-logs", 300));
        List<Block> logs = collectLogs(start, max);
        if (plugin.getConfig().getBoolean("blitz-axe.require-leaves", true) && !hasNaturalLeaves(logs)) {
            return;
        }

        BlockUtil.BUSY.add(player.getUniqueId());
        try {
            for (Block log : logs) {
                if (log.equals(start)) {
                    continue;
                }
                BlockUtil.breakAs(player, log, tool);
            }
        } finally {
            BlockUtil.BUSY.remove(player.getUniqueId());
        }
    }

    /** Alle zusammenhaengenden Staemme (inkl. diagonaler Aeste), begrenzt auf max. */
    private List<Block> collectLogs(Block start, int max) {
        List<Block> result = new ArrayList<>();
        Set<Block> seen = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && result.size() < max) {
            Block current = queue.poll();
            result.add(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block next = current.getRelative(dx, dy, dz);
                        if (!seen.contains(next) && Tag.LOGS.isTagged(next.getType())) {
                            seen.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean hasNaturalLeaves(List<Block> logs) {
        for (Block log : logs) {
            for (BlockFace face : FACES) {
                Block near = log.getRelative(face);
                Material m = near.getType();
                if (m == Material.NETHER_WART_BLOCK || m == Material.WARPED_WART_BLOCK) {
                    return true;
                }
                if (near.getBlockData() instanceof Leaves) {
                    Leaves leaves = (Leaves) near.getBlockData();
                    if (!leaves.isPersistent()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
