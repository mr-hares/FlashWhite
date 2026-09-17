package ru.mr_hares.flashWhite.events.ButtonInteraction;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ru.mr_hares.flashWhite.FlashWhite.*;

public class ButtonInteraction extends ListenerAdapter {

    private Map<String, String> getUserPlaceholders(String p, User u) {
        return new HashMap<>(Map.of(
                p + "_mention", u.getAsMention(),
                p + "_id", u.getId(),
                p + "_name", u.getName(),
                p + "_tag", u.getAsTag(),
                p + "_avatar", u.getEffectiveAvatarUrl()
        ));
    }

    private boolean hasPermission(Member member) {
        if (member == null) return false;
        if (member.hasPermission(Permission.ADMINISTRATOR)) return true;

        List<?> roleStaff = getInstance().getConfig().getList("access_control.staff_roles");
        if (roleStaff == null || roleStaff.isEmpty()) return false;

        Set<Long> staffIds = roleStaff.stream()
                .map(id -> {
                    if (id instanceof Number) {
                        return ((Number) id).longValue();
                    }
                    try {
                        return Long.parseLong(String.valueOf(id));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return member.getRoles().stream()
                .anyMatch(role -> staffIds.contains(role.getIdLong()));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String custom_id = event.getCustomId();

        if (custom_id.equals("create_ticket")) {
            if (getDB().getTicket(event.getUser().getIdLong()) != null) {
                event.reply(getMM().getString("discord.ticket_exists")).setEphemeral(true).queue();
                return;
            }

            Modal modal = new ModalBuilder(getInstance()).getModal();

            if (modal == null) {
                event.reply("Вопросы заявления не настроены. Обратитесь с этой проблемой к администрации сервера")
                        .setEphemeral(true).queue();
                return;
            }

            event.replyModal(modal).queue();

        } else if (custom_id.equals("unknow_button")) {
            event.reply("Данная кнопка не найден в списке актуальных").setEphemeral(true).queue();
        } else if (custom_id.equals("accept_ticket") || custom_id.equals("decline_ticket")) {
            event.deferReply(true).queue();

            if (!hasPermission(event.getMember())) {
                event.getHook().sendMessage(getMM().getString("discord.not_permission")).setEphemeral(true).queue();
                return;
            }

            if (getDB().getTicket(event.getChannelId()) == null) return;
            boolean isAccepted = custom_id.equals("accept_ticket");
            String verdict = isAccepted ? "Одобрить" : "Отказать";

            Map<String, String> replaces = getUserPlaceholders("admin", event.getUser());
            replaces.put("verdict", verdict);

            MessageCreateBuilder msg_verdict = getBuilder().getVerdictMessage(replaces);
            if (msg_verdict == null || msg_verdict.isEmpty()) {
                event.getHook().sendMessage("Шаблон сообщения msg_verdict.yml пуст или содержит ошибки.").setEphemeral(true).queue();
                return;
            }

            String[] ticket = getDB().getTicket(event.getChannelId());

            CompletableFuture.runAsync(() -> {
                event.getGuild().retrieveMemberById(ticket[0]).queue(retrieved -> {
                    replaces.putAll(getUserPlaceholders("user", retrieved.getUser()));

                    retrieved.getUser().openPrivateChannel().queue(privateChannel -> {
                        privateChannel.sendMessage(msg_verdict.build()).queue();
                    });

                    Role role = event.getGuild().getRoleById(getInstance().getConfig().getLong("access_control" +
                            ".reward_role_id", 0));
                    if (role != null) {
                        try {
                            event.getGuild().addRoleToMember(retrieved, role).queue();
                        } catch (Exception e) {
                            Bukkit.getConsoleSender().sendMessage(colorize("&c[FlashWhite] Error give reward role"));
                        }
                    }

                    if (!Objects.equals(ticket[1], "null") || isAccepted) {
                        getDB().addPlayer(ticket[1], "[Discord] " + event.getUser().getName(), ticket[0]);
                        Bukkit.getConsoleSender().sendMessage(colorize("[FlashWhite] New player has been added to the" +
                                " whitelist via Discord requests."));
                    }

                    getDB().removeTicket(event.getChannelId());
                    event.getHook().sendMessage(getMM().getString("discord.ticket_close")).setEphemeral(true).queue();
                    event.getChannel().delete().queueAfter(5, TimeUnit.SECONDS);

                    MessageCreateBuilder msg_log = getBuilder().getLogMessage(replaces);
                    if (!(msg_log == null || msg_log.isEmpty()) || log_channel != null) {
                        log_channel.sendMessage(msg_log.build()).queue();
                    }
                });
            });
        }
    }
}
