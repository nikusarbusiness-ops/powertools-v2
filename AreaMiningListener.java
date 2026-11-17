package de.powertools;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Blitz Drill, Blitz Pickaxe (3x3 Abbau) und Blitz Schaufel (3x3 Sand/Erde).
 * Schleichen (Shift) = nur ein einzelner Block.
 */
public class AreaMiningListener implements Listener {

    private static final Set<Material> NEVER = EnumSet.of(
            Material.BEDROCK, Material.SPAWNER, Material.TRIAL_SPAWNER,
            Material.END_PORTAL_FRAME, Material.REINFORCED_DEEPSLATE, Material.BARRIER);

    private final PowerTools plugin;
    private final Set<Material> shovelBlocks = EnumSet.noneOf(Material.class);

    public AreaMiningListener(PowerTools plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        shovelBlocks.clear();
        for (String name : plugin.getConfig().getStringList("blitz-schaufel.blocks")) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                shovelBlocks.add(m);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (BlockUtil.BUSY.contains(player.getUniqueId())) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        ItemManager.Type type = plugin.items().getType(tool);
        if (type != ItemManager.Type.BLITZ_DRILL
                && type != ItemManager.Type.BLITZ_PICKAXE
                && type != ItemManager.Type.BLITZ_SCHAUFEL) {
            return;
        }
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
            return;
        }
        if (player.isSneaking()) {
            return;
        }

        Block center = event.getBlock();
        boolean shovel = type == ItemManager.Type.BLITZ_SCHAUFEL;
        if (shovel && !shovelBlocks.contains(center.getType())) {
            return;
        }

        int depth = 1;
        if (type == ItemManager.Type.BLITZ_DRILL) {
            depth = Math.max(1, plugin.getConfig().getInt("blitz-drill.depth", 3));
        } else if (type == ItemManager.Type.BLITZ_PICKAXE) {
            depth = Math.max(1, plugin.getConfig().getInt("blitz-pickaxe.depth", 1));
        }

        BlockFace face = hitFace(player, center);
        boolean preferredOnly = plugin.getConfig().getBoolean("only-preferred-tool", true);

        List<Block> targets = new ArrayList<>();
        for (int d = 0; d < depth; d++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (d == 0 && a == 0 && b == 0) {
                        continue;
                    }
                    int ox;
                    int oy;
                    int oz;
                    if (face.getModY() != 0) {
                        ox = a;
                        oz = b;
                        oy = -face.getModY() * d;
                    } else if (face.getModX() != 0) {
                        oy = a;
                        oz = b;
                        ox = -face.getModX() * d;
                    } else {
                        ox = a;
                        oy = b;
                        oz = -face.getModZ() * d;
                    }
                    Block target = center.getRelative(ox, oy, oz);
                    if (isValidTarget(target, tool, shovel, preferredOnly)) {
                        targets.add(target);
                    }
                }
            }
        }

        BlockUtil.BUSY.add(player.getUniqueId());
        try {
            for (Block target : targets) {
                BlockUtil.breakAs(player, target, tool);
            }
        } finally {
            BlockUtil.BUSY.remove(player.getUniqueId());
        }
    }

    private boolean isValidTarget(Block target, ItemStack tool, boolean shovel, boolean preferredOnly) {
        Material m = target.getType();
        if (m.isAir() || target.isLiquid() || NEVER.contains(m)) {
            return false;
        }
        if (m.getHardness() < 0) {
            return false;
        }
        if (shovel) {
            return shovelBlocks.contains(m);
        }
        if (target.getState() instanceof Container) {
            return false;
        }
        return !preferredOnly || target.isPreferredTool(tool);
    }

    /** Ermittelt, welche Seite des Blocks der Spieler angeschaut hat. */
    private BlockFace hitFace(Player player, Block block) {
        RayTraceResult result = player.rayTraceBlocks(6.0);
        if (result != null && block.equals(result.getHitBlock()) && result.getHitBlockFace() != null) {
            return result.getHitBlockFace();
        }
        float pitch = player.getLocation().getPitch();
        if (pitch > 45) {
            return BlockFace.UP;
        }
        if (pitch < -45) {
            return BlockFace.DOWN;
        }
        float yaw = player.getLocation().getYaw();
        yaw = ((yaw % 360) + 360) % 360;
        BlockFace look;
        if (yaw >= 45 && yaw < 135) {
            look = BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            look = BlockFace.NORTH;
        } else if (yaw >= 225 && yaw < 315) {
            look = BlockFace.EAST;
        } else {
            look = BlockFace.SOUTH;
        }
        return look.getOppositeFace();
    }
}
