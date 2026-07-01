package ctx.mr_hares.flashWhite.minecraft.listener;

import ctx.mr_hares.flashWhite.FlashWhite;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerLogin implements Listener {

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (FlashWhite.getDataBase().isWhite(event.getPlayer().getName())) {
            if (FlashWhite.getDataBase().getUUID(event.getPlayer().getName()) == null) {
                FlashWhite.getDataBase().SetUUID(event.getPlayer().getName(), event.getPlayer().getUniqueId());
                FlashWhite.getInstance().getLogger().info("(FlashWhite) Updating data for " + event.getPlayer().getName() +
                        " (Adding player UUID)");
            }
            return;
        }
        if (!(event.getPlayer().isOp() || FlashWhite.getDataBase().isWhite(event.getPlayer().getUniqueId()))) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, FlashWhite.color(String.join("\n",
                    FlashWhite.getLocale().getStringList("not-in-whitelist"))));
        }

    }
}
