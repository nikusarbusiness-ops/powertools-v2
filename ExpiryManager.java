package de.powertools;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Laesst PowerTools-Items nach der eingestellten Zeit zerfallen und zeigt die Restzeit in der Actionbar. */
public class ExpiryManager {

    private final PowerTools plugin;

    public ExpiryManager(PowerTools plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> scan(), 20L, 20L);
    }

    private void scan() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean removed = removeExpired(player.getInventory());
            removed |= removeExpired(player.getEnderChest());
            if (removed) {
                notifyExpired(player);
            }
            showTimer(player);
        }
    }

    private boolean removeExpired(Inventory inventory) {
        boolean removed = false;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && plugin.items().isExpired(item)) {
                inventory.setItem(i, null);
                removed = true;
            }
        }
        return removed;
    }

    private void notifyExpired(Player player) {
        PowerTools.msg(player, "&cEines deiner Blitz-Tools ist zu Staub zerfallen!");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f);
    }

    private void showTimer(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemManager.Type type = plugin.items().getType(hand);
        if (type == null) {
            return;
        }
        Long expiry = plugin.items().getExpiry(hand);
        if (expiry == null) {
            return;
        }
        long left = expiry - System.currentTimeMillis();
        PowerTools.actionBar(player, type.displayName + " &7| Zerfällt in &e" + ItemManager.formatRemaining(left));
    }
}
