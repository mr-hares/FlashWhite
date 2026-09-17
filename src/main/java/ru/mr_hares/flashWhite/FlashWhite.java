package ru.mr_hares.flashWhite;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.mr_hares.flashWhite.commands.fw_command;
import ru.mr_hares.flashWhite.events.ButtonInteraction.ButtonInteraction;
import ru.mr_hares.flashWhite.events.ModalInteraction;
import ru.mr_hares.flashWhite.events.SlashCommandInteraction;
import ru.mr_hares.flashWhite.listeners.PlayerLogin;
import ru.mr_hares.flashWhite.utils.DataBase;
import ru.mr_hares.flashWhite.utils.DiscordBuilder;
import ru.mr_hares.flashWhite.utils.MessageManager;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FlashWhite extends JavaPlugin {
    private static FlashWhite instance;
    private static DataBase db;
    private static MessageManager mm;
    private static JDA jda = null;
    private static DiscordBuilder builder;
    public static TextChannel log_channel = null;

    @Override
    public void onEnable() {
        getServer().getConsoleSender().sendMessage(colorize(
                "[FlashWhite]&f&r" +
                "\n[FlashWhite]    FLASHWHITE &7- v" + getDescription().getVersion() +
                "\n[FlashWhite]    Running on " + Bukkit.getName() +
                "\n[FlashWhite]    Enabled plugin" +
                "\n[FlashWhite]&f&r"));

        instance = this;
        saveDefaultConfig();
        mm = new MessageManager(this);
        builder = new DiscordBuilder(this);

        if (getConfig().getString("integration.bot_token", "YOUR_BOT_TOKEN").equals("YOUR_BOT_TOKEN")) {
            getServer().getConsoleSender().sendMessage(colorize("&c[FlashWhite] Set your bot’s token in config.yml"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        } else {
            try {
                jda = JDABuilder.createDefault(getConfig().getString("integration.bot_token"))
                        .enableIntents(
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.GUILD_MEMBERS
                        )
                        .addEventListeners(new SlashCommandInteraction(), new ButtonInteraction(), new ModalInteraction())
                        .setStatus(OnlineStatus.IDLE).build();

                CompletableFuture.runAsync(() -> {
                    try {
                        jda.awaitReady();
                        jda.updateCommands().addCommands(
                                Commands.slash("setup_panel", "Инициализация системы").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        ).queue();
                        jda.getApplicationManager().setDescription("**Бот-заявочник** при поддержке плагина " +
                                "FlashWhite и " +
                                "Авиасейлс").queue();

                        TextChannel log_channel = jda.getTextChannelById(getConfig().getLong("audit_logging" +
                                ".log_channel_id", 0));
                        if (log_channel != null) FlashWhite.log_channel = log_channel;
                    } catch (Exception e) {
                        getServer().getConsoleSender().sendMessage(colorize("&c[FlashWhite] Error load discord bot"));
                    }
                }).join();
            } catch (Exception e) {
                getServer().getConsoleSender().sendMessage(colorize("&c[FlashWhite] Error load discord bot"));
            }
        }

        try {
            db = new DataBase(this);
            getServer().getConsoleSender().sendMessage(colorize("[FlashWhite] Loaded database SQLite"));
        } catch (SQLException e) {
            getServer().getConsoleSender().sendMessage(colorize("&c[FlashWhite] Error load database SQLite"));
        }
        registerEvents(new PlayerLogin());

        new fw_command();
    }

    private void registerEvents(Listener... listeners) {
        PluginManager pm = Bukkit.getPluginManager();
        for (Listener listener: listeners) {
            pm.registerEvents(listener, this);
        }

        String names = String.join(", ", Arrays.stream(listeners).map(listener -> listener.getClass().getSimpleName()).toList());
        getLogger().info("Loaded events: " + names);
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(colorize(
                "[FlashWhite]&f&r" +
                        "\n[FlashWhite]    FLASHWHITE &7- v" + getDescription().getVersion() +
                        "\n[FlashWhite]    Running on " + Bukkit.getName() +
                        "\n[FlashWhite]    Disabled plugin" +
                        "\n[FlashWhite]&f&r"));
    }

    public static FlashWhite getInstance() { return instance; }
    public static DataBase getDB() { return db; }
    public static MessageManager getMM() { return mm; }
    public static JDA getJDA() { return jda; }

    public static DiscordBuilder getBuilder() { return builder; }

    public static String colorize(String msg) {
        String prefix = getMM() == null ? "#6666ff&lFlashWhite | &f" : getMM().getString("prefix");
        msg = msg.replace("{prefix}", prefix);

        Matcher match = Pattern.compile("#[a-fA-F0-9]{6}").matcher(msg);
        while (match.find()) {
            String color = msg.substring(match.start(), match.end());
            msg = msg.replace(color, String.valueOf(ChatColor.of(color)));
            match = Pattern.compile("#[a-fA-F0-9]{6}").matcher(msg);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
