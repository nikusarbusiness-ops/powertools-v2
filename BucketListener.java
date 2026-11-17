package de.powertools;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

/**
 * Blitz Eimer: speichert bis zu 21 Eimer Wasser (einstellbar).
 * Rechtsklick auf eine Wasserquelle = aufnehmen, Rechtsklick sonst = Wasser platzieren.
 */
public class BucketListener implements Listener {

    private final PowerTools plugin;

    public BucketListener(PowerTools plugin) {
        this.plugin = plugin;
    }

    // Kein ignoreCancelled: Bukkit markiert Rechtsklicks in die Luft von Haus aus als "cancelled".
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // Von Schutz-Plugins gesperrte Block-Klicks nicht anfassen
        if (action == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (plugin.items().getType(item) != ItemManager.Type.BLITZ_EIMER) {
            return;
        }

        // Normales Eimer-Verhalten immer abschalten
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        int capacity = plugin.items().capacity();
        int stored = plugin.items().getWater(item);

        // 1) Schaut der Spieler auf eine Wasserquelle? -> aufnehmen
        RayTraceResult fluidHit = player.rayTraceBlocks(5.0, FluidCollisionMode.SOURCE_ONLY);
        if (fluidHit != null && fluidHit.getHitBlock() != null && isWaterSource(fluidHit.getHitBlock())) {
            if (stored >= capacity) {
                PowerTools.actionBar(player, "&cDer Blitz Eimer ist voll &7(" + stored + "/" + capacity + ")");
                return;
            }
            Block source = fluidHit.getHitBlock();
            BlockBreakEvent check = new BlockBreakEvent(source, player);
            Bukkit.getPluginManager().callEvent(check);
            if (check.isCancelled()) {
                return;
            }
            source.setType(Material.AIR);
            plugin.items().setWater(item, stored + 1);
            player.getInventory().setItemInMainHand(item);
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
            PowerTools.actionBar(player, "&bWasser: &f" + (stored + 1) + "&7/&f" + capacity);
            return;
        }

        // 2) Sonst: Wasser platzieren
        if (stored <= 0) {
            PowerTools.actionBar(player, "&cDer Blitz Eimer ist leer &7(0/" + capacity + ")");
            return;
        }
        RayTraceResult hit = player.rayTraceBlocks(5.0);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            return;
        }
        World world = player.getWorld();
        if (world.getEnvironment() == World.Environment.NETHER) {
            PowerTools.actionBar(player, "&cIm Nether verdampft das Wasser!");
            return;
        }
        Block against = hit.getHitBlock();
        Block target = against.getRelative(hit.getHitBlockFace());
        if (!(target.getType().isAir() || target.isReplaceable()) || isWaterSource(target)) {
            return;
        }

        BlockState replaced = target.getState();
        BlockPlaceEvent place = new BlockPlaceEvent(target, replaced, against, item, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(place);
        if (place.isCancelled() || !place.canBuild()) {
            return;
        }
        target.setType(Material.WATER);
        plugin.items().setWater(item, stored - 1);
        player.getInventory().setItemInMainHand(item);
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
        PowerTools.actionBar(player, "&bWasser: &f" + (stored - 1) + "&7/&f" + capacity);
    }

    private boolean isWaterSource(Block block) {
        if (block.getType() != Material.WATER) {
            return false;
        }
        if (block.getBlockData() instanceof Levelled) {
            return ((Levelled) block.getBlockData()).getLevel() == 0;
        }
        return false;
    }

    /** Der Blitz Eimer darf nie ueber die normale Eimer-Mechanik gefuellt werden. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (plugin.items().getType(main) == ItemManager.Type.BLITZ_EIMER
                || plugin.items().getType(off) == ItemManager.Type.BLITZ_EIMER) {
            event.setCancelled(true);
        }
    }

    /** Verhindert, dass Werfer/Spender den Blitz Eimer verbrauchen oder umwandeln. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDispense(BlockDispenseEvent event) {
        if (plugin.items().getType(event.getItem()) != null) {
            event.setCancelled(true);
        }
    }
}
