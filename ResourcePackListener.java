package de.powertools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Schickt optional das Texturen-Resourcepack an Spieler, die joinen. */
public class ResourcePackListener implements Listener {

    private final PowerTools plugin;

    public ResourcePackListener(PowerTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) {
            return;
        }
        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        Player player = event.getPlayer();
        String sha1 = plugin.getConfig().getString("resource-pack.sha1", "");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            byte[] hash = parseSha1(sha1);
            if (hash != null) {
                player.setResourcePack(url, hash);
            } else {
                player.setResourcePack(url);
            }
        }, 20L);
    }

    private byte[] parseSha1(String hex) {
        if (hex == null) {
            return null;
        }
        String h = hex.trim();
        if (h.length() != 40) {
            return null;
        }
        byte[] out = new byte[20];
        try {
            for (int i = 0; i < 20; i++) {
                out[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return out;
    }
}
