package ru.mr_hares.flashWhite.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

import static ru.mr_hares.flashWhite.FlashWhite.*;

public class PlayerLogin implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerLoginEvent event) {
        boolean enabled_whitelist = getInstance().getConfig().getBoolean("enabled", true);
        Object[] infoplayer = getDB().getInfoPlayer(event.getPlayer().getName(), event.getPlayer().getUniqueId());

        if (!enabled_whitelist || event.getPlayer().hasPermission("flashwhite.bypass")) return;

        if (infoplayer == null) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, String.join("\n", getMM().getStringList("kick.message")));
            return;
        }

        if (infoplayer[1] == "null") {
            getDB().setUUID(event.getPlayer().getName(), event.getPlayer().getUniqueId());
            return;
        }

        try {
            if (!UUID.fromString((String) infoplayer[1]).equals(event.getPlayer().getUniqueId())) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, String.join("\n", getMM().getStringList("kick.message")));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
