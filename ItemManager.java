package de.powertools;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemManager {

    public enum Type {
        BLITZ_DRILL("blitz_drill", Material.NETHERITE_PICKAXE, "&b&lBlitz Drill", true,
                "&8&oGeschmiedet im Auge des Sturms.",
                "",
                "&7Ein Schlag, &bneun Blöcke &7- 3x3-Tunnel",
                "&7Blitzschnell durch Stein, Erz & Erde"),
        BLITZ_PICKAXE("blitz_pickaxe", Material.DIAMOND_PICKAXE, "&9&lBlitz Pickaxe", true,
                "&8&oEin Funke genügt.",
                "",
                "&7Baut &b3x3 Blöcke &7auf einmal ab",
                "&7Spitzhacke mit &eEffizienz V"),
        BLITZ_AXE("blitz_axe", Material.NETHERITE_AXE, "&3&lBlitz Axe", true,
                "&8&oDer Wald verneigt sich vor dem Blitz.",
                "",
                "&7Fällt &2ganze Bäume &7mit einem Schlag",
                "&7Kein Stamm bleibt stehen"),
        BLITZ_SELL_AXE("blitz_sell_axe", Material.GOLDEN_AXE, "&6&lBlitz Sell Axe", false,
                "&8&oAlles, was sie berührt, wird zu Gold.",
                "",
                "&7Rechtsklick auf eine Kiste:",
                "&7verkauft den &6gesamten Inhalt"),
        BLITZ_SCHAUFEL("blitz_schaufel", Material.NETHERITE_SHOVEL, "&b&lBlitz Schaufel", true,
                "&8&oSchneller als der Sand rieseln kann.",
                "",
                "&7Gräbt &b3x3 Blöcke &7Sand & Erde ab",
                "&7Mit einem einzigen Schlag"),
        BLITZ_EIMER("blitz_eimer", Material.BUCKET, "&b&lBlitz Eimer", false,
                "&8&oEs knistert vor Wasser.",
                "",
                "&7Speichert bis zu &b21 Eimer &7Wasser",
                "&7Rechtsklick: Wasser aufnehmen / platzieren");

        public final String id;
        public final Material material;
        public final String displayName;
        public final boolean efficiency;
        public final String[] lore;

        Type(String id, Material material, String displayName, boolean efficiency, String... lore) {
            this.id = id;
            this.material = material;
            this.displayName = displayName;
            this.efficiency = efficiency;
            this.lore = lore;
        }

        public static Type parse(String s) {
            if (s == null) {
                return null;
            }
            String in = s.toLowerCase(Locale.ROOT);
            for (Type t : values()) {
                if (t.id.equals(in)) {
                    return t;
                }
            }
            return null;
        }
    }

    private final PowerTools plugin;
    private final NamespacedKey keyType;
    private final NamespacedKey keyExpire;
    private final NamespacedKey keyWater;

    public ItemManager(PowerTools plugin) {
        this.plugin = plugin;
        this.keyType = new NamespacedKey(plugin, "tool_type");
        this.keyExpire = new NamespacedKey(plugin, "expire_at");
        this.keyWater = new NamespacedKey(plugin, "water");
    }

    public int capacity() {
        return Math.max(1, plugin.getConfig().getInt("blitz-eimer.capacity", 21));
    }

    public ItemStack create(Type t) {
        ItemStack item = new ItemStack(t.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PowerTools.color(t.displayName));

        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        cmd.setStrings(List.of(t.id));
        meta.setCustomModelDataComponent(cmd);

        if (t.efficiency) {
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        }
        if (t == Type.BLITZ_EIMER) {
            meta.setMaxStackSize(1);
        } else {
            meta.setUnbreakable(true);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyType, PersistentDataType.STRING, t.id);

        int days = plugin.getConfig().getInt("items.lifetime-days", 0);
        if (days > 0) {
            pdc.set(keyExpire, PersistentDataType.LONG, System.currentTimeMillis() + days * 86400000L);
        }
        if (t == Type.BLITZ_EIMER) {
            pdc.set(keyWater, PersistentDataType.INTEGER, 0);
        }

        applyLore(meta, t, 0);
        item.setItemMeta(meta);
        return item;
    }

    private void applyLore(ItemMeta meta, Type t, int water) {
        List<String> lore = new ArrayList<>();
        for (String line : t.lore) {
            lore.add(PowerTools.color(line));
        }
        if (t == Type.BLITZ_EIMER) {
            lore.add("");
            lore.add(PowerTools.color("&7Wasser: &b" + water + "&7/&b" + capacity()));
        }
        meta.setLore(lore);
    }

    public Type getType(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
        return Type.parse(id);
    }

    public Long getExpiry(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(keyExpire, PersistentDataType.LONG);
    }

    public boolean isExpired(ItemStack item) {
        if (getType(item) == null) {
            return false;
        }
        Long exp = getExpiry(item);
        return exp != null && System.currentTimeMillis() >= exp;
    }

    public int getWater(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(keyWater, PersistentDataType.INTEGER, 0);
    }

    public void setWater(ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keyWater, PersistentDataType.INTEGER, amount);
        applyLore(meta, Type.BLITZ_EIMER, amount);
        item.setItemMeta(meta);
    }

    public static String formatRemaining(long ms) {
        long totalMinutes = Math.max(0, ms) / 60000L;
        long days = totalMinutes / 1440;
        long hours = (totalMinutes % 1440) / 60;
        long minutes = totalMinutes % 60;
        if (days > 0) {
            return days + "T " + hours + "Std";
        }
        if (hours > 0) {
            return hours + "Std " + minutes + "Min";
        }
        return minutes + "Min";
    }
}
