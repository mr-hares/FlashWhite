package ctx.mr_hares.flashWhite.utils;

import ctx.mr_hares.flashWhite.FlashWhite;
import ctx.mr_hares.flashWhite.minecraft.listener.PlayerJoin;
import ctx.mr_hares.flashWhite.minecraft.listener.PlayerLogin;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EventRegistrar {

    private final JavaPlugin plugin;

    public EventRegistrar(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAllEvents() {
        PluginManager pm = plugin.getServer().getPluginManager();

        Listener[] listeners = {
                new PlayerJoin(),
                new PlayerLogin()
        };

        int count = 0;

        for (Listener listener : listeners) {
            pm.registerEvents(listener, plugin);
            count++;
        }

        FlashWhite.sendConsole("(FlashWhite) Зарегестрировано " + count + " ивентов Minecraft");
    }
}