package de.powertools;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PowerTools extends JavaPlugin {

    public static final String PREFIX = "&6[PowerTools] &r";

    private ItemManager items;
    private SellManager sell;
    private AreaMiningListener areaMining;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        items = new ItemManager(this);
        sell = new SellManager(this);
        areaMining = new AreaMiningListener(this);

        setupEconomy();

        Bukkit.getPluginManager().registerEvents(areaMining, this);
        Bukkit.getPluginManager().registerEvents(new TreeAxeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SellAxeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BucketListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ResourcePackListener(this), this);
        new ExpiryManager(this).start();

        PluginCommand cmd = getCommand("powertools");
        if (cmd != null) {
            PowerToolsCommand handler = new PowerToolsCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        if (economy() == null) {
            getLogger().warning("Kein Vault/Economy-Plugin gefunden - die Blitz Sell Axe funktioniert erst, wenn eines vorhanden ist.");
        }
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    /** Liefert die Economy (oder null). Versucht es erneut, falls sie erst spaeter registriert wurde. */
    public Economy economy() {
        if (economy == null) {
            setupEconomy();
        }
        return economy;
    }

    public ItemManager items() {
        return items;
    }

    public SellManager sell() {
        return sell;
    }

    public AreaMiningListener areaMining() {
        return areaMining;
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void msg(CommandSender sender, String text) {
        sender.sendMessage(color(PREFIX + text));
    }

    public static void actionBar(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(color(text)));
    }
}
