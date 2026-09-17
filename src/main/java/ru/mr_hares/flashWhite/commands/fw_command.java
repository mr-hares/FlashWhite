package ru.mr_hares.flashWhite.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import ru.mr_hares.flashWhite.utils.CommandTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.mr_hares.flashWhite.FlashWhite.*;

public class fw_command extends CommandTemplate {
    public fw_command() {
        super("flashwhite", getInstance());
    }

    private boolean isInteger(String str) {
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

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colorize(String.join("\n", getMM().getStringList("help.commands"))));
            return;
        }

        if (args[0].equals("on") || args[0].equals("off")) {
            boolean enabled = args[0].equals("on");

            getInstance().getConfig().set("enabled", enabled);
            getInstance().saveConfig();

            sender.sendMessage(colorize(getMM().getString("system.toggle_" + (enabled ? "on" : "off"))));
        } else if (args[0].equals("add")) {
            if (args.length < 2) {
                sender.sendMessage(colorize(getMM().getString("help.syntax.add")));
                return;
            }

            if (getDB().getInfoPlayer(args[1], UUID.randomUUID()) != null) {
                sender.sendMessage(colorize(getMM().getString("players.already_in")));
                return;
            }

            getDB().addPlayer(args[1], "null", "null");
            sender.sendMessage(colorize(getMM().getString("players.added")
                            .replace("{player}", args[1])
                            .replace("{time}", getMM().getString("time.forever"))
                    ));
        } else if (args[0].equals("addtemp")) {
            if (args.length < 3) {
                sender.sendMessage(colorize(getMM().getString("help.syntax.addtemp")));
                return;
            }

            if (getDB().getInfoPlayer(args[1], UUID.randomUUID()) != null) {
                sender.sendMessage(colorize(getMM().getString("players.already_in")));
                return;
            }

            int days = 0;
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            String time_formatted = null;
            String time = args[2].substring(0, args[2].length() - 1);
            if (!isInteger(time)) {
                sender.sendMessage(colorize(getMM().getString("use-add")));
                return;
            } else {
                if (args[2].endsWith("d")) {
                    days = Integer.parseInt(time);
                    time_formatted = getMM().getString("time.d").replace("{time}",
                            time);
                } else if (args[2].endsWith("h")) {
                    hours = Integer.parseInt(time);
                    time_formatted = getMM().getString("time.h").replace("{time}",
                            time);
                } else if (args[2].endsWith("m")) {
                    minutes = Integer.parseInt(time);
                    time_formatted = getMM().getString("time.m").replace("{time}",
                            time);
                } else if (args[2].endsWith("s")) {
                    seconds = Integer.parseInt(time);
                    time_formatted = getMM().getString("time.s").replace("{time}",
                            time);
                } else {
                    sender.sendMessage(ChatColor.RED + "Ошибка при выполнение команды (TimeError: 956)");
                    return;
                }
            }

            getDB().addTempPlayer(args[1], "null", "null", seconds, minutes, hours, days);
            sender.sendMessage(colorize(getMM().getString("players.added")
                    .replace("{player}", args[1])
                    .replace("{time}", time_formatted)
            ));
        } else if (args[0].equals("remove")) {
            if (args.length < 2) {
                sender.sendMessage(colorize(getMM().getString("help.syntax.remove")));
                return;
            }

            if (getDB().getInfoPlayer(args[1], UUID.randomUUID()) == null) {
                sender.sendMessage(colorize(getMM().getString("players.not_found")));
                return;
            }

            getDB().removePlayer(args[1]);
            sender.sendMessage(colorize(getMM().getString("players.removed").replace("{player}", args[1])));
        } else if (args[0].equals("check")) {
            if (args.length < 2) {
                sender.sendMessage(colorize(getMM().getString("help.syntax.check")));
                return;
            }

            Object[] infoplayers = getDB().getInfoPlayer(args[1], UUID.randomUUID());
            sender.sendMessage(colorize(getMM().getString("check.header").replace("{player}", args[1])));
            List<String> message = getMM().getStringList("check.info").stream().map(line -> colorize(line
                    .replace("{status}", infoplayers == null ? getMM().getString("check.status.not-in-whitelist") : getMM().getString("check.status.in-whitelist"))
                    .replace("{uuid}", infoplayers == null ? "null" : String.valueOf(infoplayers[1]))
                    .replace("{moder}", infoplayers == null ? "null" : (String) infoplayers[2])
                    .replace("{discord_id}", infoplayers == null ? "null" : (String) infoplayers[4]))
            ).toList();
            
            sender.sendMessage(String.join("\n", message));
        } else if (args[0].equalsIgnoreCase("list")) {
            int page = 1;

            if (args.length == 2) {
                if (isInteger(args[1])) {
                    page = Integer.parseInt(args[1]);
                }
            }

            if (page < 1 || page > (int) Math.ceil((double) getDB().getPlayers().size() / 5)) {
                sender.sendMessage(colorize(getMM().getString("system.invalid_page")));
                return;
            }

            sender.sendMessage(colorize(getMM().getString("list.header").replace("{page}",
                    String.valueOf(page))));
            List<String[]> players = getDB().getPlayers();
            for (String[] s: players.subList((page-1)*5, Math.min(page*5,
                    players.size()))) {
                sender.sendMessage(colorize(String.join("\n", getMM().getStringList("list.item")).replace("{player}",
                        s[1]).replace("{moder}", s[2]).replace("{uuid}", s[0])));
            }
        } else if (args[0].equals("reload")) {
            getInstance().reloadConfig();
            getMM().reload();

            sender.sendMessage(colorize(getMM().getString("system.reload")));
        } else {
            sender.sendMessage(colorize(String.join("\n", getMM().getStringList("help.commands"))));
            return;
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        List<String> subcommands = new ArrayList<>(List.of("add", "remove", "list", "addtemp", "on", "off", "check"));
        if (sender.hasPermission("flashwhite.reload")) subcommands.add("reload");
        return subcommands;
    }
}
