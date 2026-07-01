package ctx.mr_hares.flashWhite.command;

import ctx.mr_hares.flashWhite.CommandTemplate;
import ctx.mr_hares.flashWhite.discord.listener.ButtonInteraction;
import ctx.mr_hares.flashWhite.discord.listener.ModalInteraction;
import ctx.mr_hares.flashWhite.discord.listener.SlashCommandInteraction;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ctx.mr_hares.flashWhite.FlashWhite.*;

public class flashwhite extends CommandTemplate {

    public flashwhite() {
        super("flashwhite", getInstance());
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("use")) {
            sender.sendMessage(color(getLocale().getString("not-permission")));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(color(String.join("\n", getLocale().getStringList("help.command-list"))));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("reload")) {
                sender.sendMessage(color(getLocale().getString("not-permission")));
                return;
            }
            long start = System.currentTimeMillis();
            sender.sendMessage(color(getLocale().getString("reload-start")));
            getInstance().reloadConfig();
            getInstance().reloadLocale();

            if (getInstance().getConfig().getString("discord.bot-token") == null ||
                    (getInstance().getConfig().getString("discord.bot-token") != null && Objects.equals(getInstance().getConfig().getString("discord.bot-token"), "YOUR_BOT_TOKEN"))) {
                getInstance().getLogger().warning(ChatColor.RED + "Specify the Discord bot token in the config.yml file");
            } else {
                if (getJda() != null) {
                    getJda().shutdown();
                    setJDA(null);
                }

                try {
                    JDA jda = JDABuilder.createDefault(getInstance().getConfig().getString("discord.bot-token"))
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
                            jda.awaitReady();
                            jda.updateCommands().addCommands(
                                    Commands.slash("setup", "Инициализация системы").setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))).queue();
                            jda.getApplicationManager().setDescription("**Бот-заявочник** при поддержке плагина " +
                                    "flashWhite и Авиасейлс").queue();
                            setJDA(jda);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (Exception e) {
                    getInstance().getLogger().warning(ChatColor.RED + "Error initializing the Discord bot");
                }
            }

            sender.sendMessage(color(getLocale().getString("reload-end").replace("{time}", String.valueOf((System.currentTimeMillis() - start) / 1000))));
            return;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                sender.sendMessage(color(getLocale().getString("use-add")));
                return;
            }

            int days = 0;
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            String time_formatted = getLocale().getString("time.forever");

            if (args.length == 3) {
                String time = args[2].substring(0, args[2].length() - 1);
                if (!isInteger(time)) {
                    sender.sendMessage(color(getLocale().getString("use-add")));
                    return;
                } else {
                    if (args[2].endsWith("d")) {
                        days = Integer.parseInt(time);
                        time_formatted = getLocale().getString("time.d", "{time} д.").replace("{time}",
                                time);
                    } else if (args[2].endsWith("h")) {
                        hours = Integer.parseInt(time);
                        time_formatted = getLocale().getString("time.h", "{time} ч.").replace("{time}",
                                time);
                    } else if (args[2].endsWith("m")) {
                        minutes = Integer.parseInt(time);
                        time_formatted = getLocale().getString("time.m", "{time} м.").replace("{time}",
                                time);
                    } else if (args[2].endsWith("s")) {
                        seconds = Integer.parseInt(time);
                        time_formatted = getLocale().getString("time.s", "{time} сек.").replace("{time}",
                                time);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Ошибка при выполнение команды (TimeError: 956)");
                        return;
                    }
                }
            }
            if (getDataBase().isWhite(args[1])) {
                sender.sendMessage(color(getLocale().getString("player-in-whitelist")));
                return;
            }
            if (days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
                getDataBase().addPlayer("not", args[1], sender.getName());
            } else {
                getDataBase().addTempPlayer("not", args[1], sender.getName(), days, hours, minutes, seconds);
            }
            sender.sendMessage(color(getLocale().getString("player-add").replace("{time}", time_formatted).replace("{player}",
                    args[1])));
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 2) {
                sender.sendMessage(color(getLocale().getString("use-remove")));
                return;
            }
            if (!getDataBase().isWhite(args[1])) {
                sender.sendMessage(color(getLocale().getString("player-not-in-whitelist")));
                return;
            }
            getDataBase().removePlayer(args[1]);
            sender.sendMessage(color(getLocale().getString("player-remove").replace("{player}",
                    args[1])));
            return;
        } else if (args[0].equalsIgnoreCase("list")) {
            int page = 1;

            if (args.length == 2) {
                if (isInteger(args[1])) {
                    page = Integer.parseInt(args[1]);
                }
            }

            if (page < 1 || page > (int) Math.ceil((double) getDataBase().getPlayers().size() / 5)) {
                sender.sendMessage(color(getLocale().getString("not-found-page")));
                return;
            }

            sender.sendMessage(color(getLocale().getString("list.header").replace("{page}",
                    String.valueOf(page))));
            List<String[]> players = getDataBase().getPlayers();
            for (String[] s: players.subList((page-1)*5, Math.min(page*5,
                    players.size()))) {
                sender.sendMessage(color(String.join("\n", getLocale().getStringList("list.item")).replace("{player}", s[1]).replace("{moder}", s[2]).replace("{uuid}", s[0])));
            }
        } else {
            sender.sendMessage(color(String.join("\n", getLocale().getStringList("help.command-list"))));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (sender.hasPermission("reload")) {
                commands.add("reload");
            }
            commands.add("add");
            commands.add("remove");
            commands.add("list");
            return commands;
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("add") && isInteger(args[2])) {
                if (!(args[2].endsWith("d") || args[2].endsWith("h") || args[2].endsWith("m") || args[2].endsWith("s"))) {
                    List<String> times = new ArrayList<>();
                    times.add(args[2] + "d");
                    times.add(args[2] + "h");
                    times.add(args[2] + "m");
                    times.add(args[2] + "s");
                    return times;
                }
            }
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove")) {
                List<String> nicks = new ArrayList<>();
                for (String[] s: getDataBase().getPlayers()) {
                    nicks.add(s[1]);
                }
                return nicks;
            }
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list")) {
                List<String[]> players = getDataBase().getPlayers();
                List<String> pages = new ArrayList<>();

                int pageCount = (int) Math.ceil(players.size() / 5.0);
                pages = IntStream.rangeClosed(1, pageCount)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.toList());

                return pages;
            }
        }
        return new ArrayList<>();
    }
}
