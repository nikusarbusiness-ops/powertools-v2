package de.powertools;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Blitz Sell Axe: Rechtsklick auf eine Kiste verkauft den gesamten Inhalt. */
public class SellAxeListener implements Listener {

    private final PowerTools plugin;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public SellAxeListener(PowerTools plugin) {
        this.plugin = plugin;
    }

    // ignoreCancelled: Schutz-Plugins (WorldGuard, Lands, ...) haben Vorrang
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (plugin.items().getType(event.getItem()) != ItemManager.Type.BLITZ_SELL_AXE) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container)) {
            return;
        }
        Container container = (Container) block.getState();

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);

        Player player = event.getPlayer();
        if (!player.hasPermission("powertools.sellaxe")) {
            PowerTools.msg(player, "&cDafür hast du keine Berechtigung.");
            return;
        }

        long now = System.currentTimeMillis();
        long wait = plugin.getConfig().getLong("blitz-sell-axe.cooldown-ms", 500);
        Long last = cooldown.get(player.getUniqueId());
        if (last != null && now - last < wait) {
            return;
        }
        cooldown.put(player.getUniqueId(), now);

        Economy economy = plugin.economy();
        if (economy == null) {
            PowerTools.msg(player, "&cKein Economy-Plugin (Vault) gefunden.");
            return;
        }

        SellManager.Result result = plugin.sell().sell(player, container.getInventory());
        if (result.items <= 0) {
            PowerTools.msg(player, "&cIn dieser Kiste ist nichts Verkäufliches.");
            return;
        }
        PowerTools.msg(player, "&aVerkauft: &e" + result.items + " Items &afür &6" + economy.format(result.total));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }
}
