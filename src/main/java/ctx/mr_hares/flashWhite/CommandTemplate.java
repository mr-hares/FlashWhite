package ctx.mr_hares.flashWhite;

import org.bukkit.command.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandTemplate implements CommandExecutor, TabCompleter {
    public CommandTemplate(String command, JavaPlugin plugin) {
        PluginCommand pluginCommand = plugin.getCommand(command);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(this);
        }
    }

    public abstract void execute(CommandSender sender, String label, String[] args);

    public abstract List<String> complete(CommandSender sender, String[] args);

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        execute(sender, label, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, @NonNull String[] args) {
        return filter(complete(sender, args), args);
    }

    private List<String> filter(List<String> list, String[] args) {
        if (list == null) return null;
        String last = args[args.length - 1];
        List<String> result = new ArrayList<>();
        for (String arg : list) {
            if (arg.toLowerCase().startsWith(last.toLowerCase())) result.add(arg);
        }

        return result;
    }
}
