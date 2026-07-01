package ctx.mr_hares.flashWhite;

import ctx.mr_hares.flashWhite.command.flashwhite;
import ctx.mr_hares.flashWhite.discord.listener.ButtonInteraction;
import ctx.mr_hares.flashWhite.discord.listener.ModalInteraction;
import ctx.mr_hares.flashWhite.discord.listener.SlashCommandInteraction;
import ctx.mr_hares.flashWhite.minecraft.listener.PlayerJoin;
import ctx.mr_hares.flashWhite.minecraft.listener.PlayerLogin;
import ctx.mr_hares.flashWhite.utils.DataBase;
import net.dv8tion.jda.api.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FlashWhite extends JavaPlugin {
    private static FlashWhite instance;
    private static JDA jda = null;
    private static DataBase dataBase;
    private static YamlConfiguration locale;
    public static TextChannel logChannel = null;

    @Override
    public void onEnable() {
        sendConsole("&f&r\n  &#6666ffFlashWhite &7by mr_hares&r\n  &fEnable plugin&r\n  &7Version " + getDescription().getVersion() + "&r\n&f&r");

        instance = this;
        saveDefaultConfig();
        InputStream defConfig = getResource("config.yml");
        if (defConfig != null) {
            getConfig().setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfig,
                    StandardCharsets.UTF_8)));
            try {
                getConfig().options().copyDefaults(true);
                getConfig().save(new File(getDataFolder(), "config.yml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        File file = new File(getDataFolder(), "locale.yml");
        if (!file.exists()) {
            saveResource("locale.yml", false);
        }
        locale = YamlConfiguration.loadConfiguration(file);
        InputStream defLocale = getResource("locale.yml");
        if (defLocale != null) {
            locale.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defLocale, StandardCharsets.UTF_8)));
            try {
                locale.options().copyDefaults(true);
                locale.save(new File(getDataFolder(), "locale.yml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            dataBase = new DataBase();
            dataBase.initializeDatabase();
        } catch (Exception e) {
            getServer().getConsoleSender().sendMessage("(FlashWhite) " + ChatColor.RED + "Error creating and initializing the database");
        }

        registerEvents();

        if (getConfig().getString("discord.bot-token") == null ||
                (getConfig().getString("discord.bot-token") != null && Objects.equals(getConfig().getString("discord.bot-token"), "YOUR_BOT_TOKEN"))) {
            getServer().getConsoleSender().sendMessage("(FlashWhite) " + ChatColor.RED + "Specify the Discord bot token in the config.yml file");
        } else {
            try {
                JDA jda_tmp =
                        JDABuilder.createDefault(getConfig().getString("discord.bot-token"))
                                .addEventListeners(new SlashCommandInteraction())
                                .addEventListeners(new ModalInteraction())
                                .addEventListeners(new ButtonInteraction())
                                .enableIntents(
                                        GatewayIntent.MESSAGE_CONTENT,
                                        GatewayIntent.GUILD_MESSAGES,
                                        GatewayIntent.GUILD_MEMBERS
                                ).setStatus(OnlineStatus.DO_NOT_DISTURB).build();

                CompletableFuture.runAsync(() -> {
                    try {
                        jda_tmp.awaitReady();
                        jda_tmp.updateCommands().addCommands(
                                Commands.slash("setup", "Инициализация системы").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        ).queue();
                        jda_tmp.getApplicationManager().setDescription("**Бот-заявочник** при поддержке плагина " +
                                "FlashWhite и " +
                                "Авиасейлс").queue();
                        jda = jda_tmp;
                    } catch (InterruptedException e) {
                        getServer().getConsoleSender().sendMessage("(FlashWhite) " + ChatColor.RED + "Error initializing the Discord bot");
                    }
                }).join();
            } catch (Exception e) {
                getServer().getConsoleSender().sendMessage("(FlashWhite) " + ChatColor.RED + "Error initializing the Discord bot");
            }
        }

        for (Guild guild : jda.getGuilds()) {
            TextChannel textChannel = guild.getTextChannelById(
                    getConfig().getLong("discord.log-channel")
            );
            if (textChannel != null) {
                logChannel = textChannel;
                break;
            }
        }

        new flashwhite();
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoin(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerLogin(), this);

        FlashWhite.sendConsole("(FlashWhite) Have been 2 Minecraft events registered");
    }

    @Override
    public void onDisable() {
        sendConsole("&f&r\n  &#6666ffFlashWhite &7by mr_hares&r\n  &fDisable plugin&r\n  &7Version " + getDescription().getVersion() + "&r\n&f&r");
        if (jda != null) {
            jda.shutdown();
        }
    }

    public static FlashWhite getInstance() { return instance; }
    public static JDA getJda() { return jda; }
    public static DataBase getDataBase() { return dataBase; }
    public static YamlConfiguration getLocale() { return locale; }

    public void reloadLocale() {
        File file = new File(getDataFolder(), "locale.yml");
        locale = YamlConfiguration.loadConfiguration(file);

        for (Guild guild : jda.getGuilds()) {
            TextChannel textChannel = guild.getTextChannelById(
                    getConfig().getLong("discord.log-channel")
            );
            if (textChannel != null) {
                logChannel = textChannel;
                break;
            }
        }
    }

    public static boolean isInteger(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void setJDA(JDA jda) {
        FlashWhite.jda = jda;
    }

    public static String updateCheck() {
        try {
            URL url = new URL("https://api.github.com/repos/mr-hares/flashwhite/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            String latestVersion = json.get("tag_name").getAsString();
            String currentVersion = FlashWhite.getInstance().getDescription().getVersion();

            if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                return latestVersion;
            }
        } catch (Exception e) {
            FlashWhite.getInstance().getLogger().warning("Не удалось проверить обновления (ошибка соединения с GitHub).");
        }

        return null;
    }

    public static void sendConsole(String text) {
        Bukkit.getServer().getConsoleSender().sendMessage(color(text));
    }

    public static String replacePlaceholder(String text, ModalInteractionEvent event, User user,
                                            Map<String, String> answers) {
        if (text == null) return null;

        if (answers != null) {
            for (Map.Entry<String, String> entry : answers.entrySet()) {
                text = text.replace("{answer_" + entry.getKey() + "}", entry.getValue());
            }
        } else if (event != null) {
            Pattern pattern = Pattern.compile("\\{answer_(\\w+)\\}");
            Matcher matcher = pattern.matcher(text);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String key = matcher.group(1);
                ModalMapping value = event.getValue(key);
                if (value != null) {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(value.getAsString()));
                }
            }
            matcher.appendTail(result);
            text = result.toString();
        }

        if (user != null) {
            text = text.replace("{mention}", user.getAsMention())
                    .replace("{user_id}", user.getId())
                    .replace("{user_name}", user.getName())
                    .replace("{user_tag}", user.getAsTag())
                    .replace("{user_avatar}", user.getAvatarUrl() != null ? user.getAvatarUrl() : user.getDefaultAvatarUrl());
        }

        return text;
    }

    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        if (getLocale() != null) {
            message = message.replace("{prefix}", getLocale().getString("prefix", "&#6666ff(FlashWhite) &r"));
        }

        Matcher matcher = Pattern.compile("&#([A-Fa-f0-9]{6})").matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static EmbedBuilder getEmbed(String text) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(text);

        return embedBuilder;
    }
}
