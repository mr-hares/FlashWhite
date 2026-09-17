package ru.mr_hares.flashWhite.events;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ru.mr_hares.flashWhite.FlashWhite.*;

public class ModalInteraction extends ListenerAdapter {

    private Map<String, String> getUserPlaceholders(String p, User u) {
        return new HashMap<>(Map.of(
                p + "_mention", u.getAsMention(),
                p + "_id", u.getId(),
                p + "_name", u.getName(),
                p + "_tag", u.getAsTag(),
                p + "_avatar", u.getEffectiveAvatarUrl()
        ));
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        Map<String, String> replaces = new HashMap<>();

        event.deferReply(true).queue();

        for (ModalMapping mapping: event.getValues()) {
            replaces.put(mapping.getCustomId(), mapping.getAsString());
        }

        replaces.putAll(getUserPlaceholders("user", event.getUser()));

        MessageCreateBuilder msg_ticket = getBuilder().getTicketMessage(replaces);
        if (msg_ticket == null || msg_ticket.isEmpty()) {
            event.getHook().sendMessage("Шаблон сообщения msg_ticket.yml пуст или содержит ошибки.").setEphemeral(true).queue();
            return;
        }

        long categoryId = getInstance().getConfig().getLong("access_control.target_category_id", 0);
        Category category = event.getGuild().getCategoryById(categoryId);

        if (category == null) {
            event.getHook().sendMessage("Категория для тикетов не найдена. Обратитесь к администратору.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        CompletableFuture.runAsync(() -> {
            ChannelAction<TextChannel> channelAction =
                    category.createTextChannel(replace(getInstance().getConfig().getString("ticket_channels" +
                                    ".naming_format", "\uD83C\uDFAB-заявка-{user_name}"), getUserPlaceholders("user",
                                    event.getUser())))
                            .addPermissionOverride(event.getGuild().getPublicRole(), null, List.of(Permission.VIEW_CHANNEL))
                            .addPermissionOverride(event.getMember(), List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null);

            addStaffPermissions(channelAction, event.getGuild());

            channelAction.queue(textChannel -> {
                String nick = replaces.getOrDefault("minecraft_nick", "null");
                getDB().createTicket(textChannel.getId(), event.getUser().getId(), nick);

                textChannel.sendMessage(msg_ticket.build()).queue();
                event.getHook().sendMessage(getMM().getString("discord.ticket_created")
                                .replace("{channel}", textChannel.getAsMention()))
                        .setEphemeral(true)
                        .queue();
            }, error -> {
                event.getHook().sendMessage("При создании канала произошла ошибка: " + error.getMessage())
                        .setEphemeral(true)
                        .queue();
            });
        });
    }

    private void addStaffPermissions(ChannelAction<TextChannel> channelAction, net.dv8tion.jda.api.entities.Guild guild) {
        List<?> roleStaff = getInstance().getConfig().getList("access_control.staff_roles");
        if (roleStaff != null) {
            for (Object roleIdObj : roleStaff) {
                long roleId;
                if (roleIdObj instanceof Number) {
                    roleId = ((Number) roleIdObj).longValue();
                } else {
                    try {
                        roleId = Long.parseLong(String.valueOf(roleIdObj));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }

                if (guild.getRoleById(roleId) != null) {
                    channelAction.addPermissionOverride(
                            guild.getRoleById(roleId),
                            List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY),
                            null
                    );
                }
            }
        }
    }

    private String replace(String text, Map<String, String> replaces) {
        if (replaces == null) return text;

        for (String key: replaces.keySet()) {
            text = text.replace("{" + key + "}", replaces.get(key));
        }

        return text;
    }
}
