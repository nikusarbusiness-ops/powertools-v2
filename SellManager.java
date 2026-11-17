package de.powertools;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.Map;

/** Preise lesen und Inventare verkaufen (fuer die Blitz Sell Axe). */
public class SellManager {

    public static class Result {
        public final double total;
        public final int items;

        public Result(double total, int items) {
            this.total = total;
            this.items = items;
        }
    }

    private final PowerTools plugin;
    private final Map<Material, Double> prices = new EnumMap<>(Material.class);

    public SellManager(PowerTools plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        prices.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("prices");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Material m = Material.matchMaterial(key);
            if (m != null) {
                prices.put(m, section.getDouble(key));
            }
        }
    }

    /** Verkauft alle verkaufbaren Items des Inventars. Nicht verkaufbare bleiben liegen. */
    public Result sell(Player player, Inventory inventory) {
        Economy economy = plugin.economy();
        if (economy == null) {
            return new Result(0, 0);
        }
        double total = 0;
        int count = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Double price = prices.get(item.getType());
            if (price == null || !isPlain(item)) {
                continue;
            }
            total += price * item.getAmount();
            count += item.getAmount();
            inventory.setItem(i, null);
        }
        if (total > 0) {
            economy.depositPlayer(player, total);
        }
        return new Result(total, count);
    }

    /** Nur "normale" Items verkaufen - keine umbenannten, verzauberten oder PowerTools-Items. */
    private boolean isPlain(ItemStack item) {
        if (plugin.items().getType(item) != null) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        return !meta.hasDisplayName() && !meta.hasLore() && !meta.hasEnchants();
    }
}
