package de.powertools;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** /pt give <spieler> <typ> [menge]  und  /pt reload */
public class PowerToolsCommand implements TabExecutor {

    private final PowerTools plugin;

    public PowerToolsCommand(PowerTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("powertools.admin")) {
            PowerTools.msg(sender, "&cKeine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            PowerTools.msg(sender, "&e/pt give <spieler> <" + typeList("|") + "> [menge]");
            PowerTools.msg(sender, "&e/pt reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.sell().reload();
            plugin.areaMining().reload();
            PowerTools.msg(sender, "&aConfig neu geladen.");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                PowerTools.msg(sender, "&cNutzung: /pt give <spieler> <typ> [menge]");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                PowerTools.msg(sender, "&cSpieler nicht online.");
                return true;
            }
            ItemManager.Type type = ItemManager.Type.parse(args[2]);
            if (type == null) {
                PowerTools.msg(sender, "&cUnbekannter Typ. Möglich: " + typeList(", "));
                return true;
            }
            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    PowerTools.msg(sender, "&cMenge muss eine Zahl sein.");
                    return true;
                }
            }
            amount = Math.max(1, Math.min(amount, 36));
            for (int i = 0; i < amount; i++) {
                ItemStack item = plugin.items().create(type);
                for (ItemStack leftover : target.getInventory().addItem(item).values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                }
            }
            PowerTools.msg(sender, "&a" + amount + "x " + type.displayName + " &aan " + target.getName() + " gegeben.");
            return true;
        }

        PowerTools.msg(sender, "&cUnbekannter Unterbefehl.");
        return true;
    }

    private String typeList(String separator) {
        List<String> ids = new ArrayList<>();
        for (ItemManager.Type t : ItemManager.Type.values()) {
            ids.add(t.id);
        }
        return String.join(separator, ids);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("powertools.admin")) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("give", "reload"), out);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[1], names, out);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> ids = new ArrayList<>();
            for (ItemManager.Type t : ItemManager.Type.values()) {
                ids.add(t.id);
            }
            StringUtil.copyPartialMatches(args[2], ids, out);
        }
        Collections.sort(out);
        return out;
    }
}
