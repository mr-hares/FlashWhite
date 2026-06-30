package ctx.mr_hares.flashWhite.minecraft.listener;

import ctx.mr_hares.flashWhite.FlashWhite;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import static ctx.mr_hares.flashWhite.FlashWhite.*;

public class PlayerJoin implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().isOp()) {
            if (FlashWhite.updateCheck() != null) {
                TextComponent button = new TextComponent(color("&#6666ff&nСсылке"));
                button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("https" +
                        "://modrinth.com/plugin/flashwhite/versions")));
                button.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://modrinth" +
                        ".com/plugin/flashwhite/versions"));
                TextComponent message =
                        new TextComponent(color("&#6666ff(FlashWhite) &fВаша версия &#6666ff" + getInstance().getDescription().getVersion() + " &fустарела. Установите новую версию &#6666ff" + FlashWhite.updateCheck() + "&fпо "));
                message.addExtra(button);
                message.addExtra(" &r&f, дабы разблокировать новые возможности");
                event.getPlayer().spigot().sendMessage(button);
            }
        }
    }
}
