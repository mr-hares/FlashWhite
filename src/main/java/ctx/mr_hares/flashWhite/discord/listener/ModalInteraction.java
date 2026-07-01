package ctx.mr_hares.flashWhite.discord.listener;

import ctx.mr_hares.flashWhite.utils.EmbedBuild;
import ctx.mr_hares.flashWhite.utils.EmojiParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static ctx.mr_hares.flashWhite.FlashWhite.*;

public class ModalInteraction extends ListenerAdapter {
    private static final String CONFIG_CATEGORY_ID = "discord.category_id";
    private static final String CONFIG_TICKET_MESSAGE = "discord.ticket-message";
    private static final String CONFIG_QUESTIONS = "discord.questions";
    private static final String CONFIG_ROLE_STAFF = "discord.role_staff";

    private void addStaffPermissions(ChannelAction<TextChannel> channelAction, net.dv8tion.jda.api.entities.Guild guild) {
        List<?> roleStaff = getInstance().getConfig().getList(CONFIG_ROLE_STAFF);
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

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().equals("whitelist-questions")) return;
        String[] existingTicket = getDataBase().getTicket(event.getUser().getIdLong());
        if (existingTicket != null) return;
        event.deferReply(true).queue();

        Map<String, String> answers = new HashMap<>();
        ConfigurationSection questions = getInstance().getConfig().getConfigurationSection(CONFIG_QUESTIONS);

        if (questions != null) {
            for (String key : questions.getKeys(false)) {
                ModalMapping value = event.getValue(key);
                if (value != null && !value.getAsString().trim().isEmpty()) {
                    answers.put(key, value.getAsString().trim());
                } else if (questions.getBoolean(key + ".required", true)) {
                    event.getHook().sendMessageEmbeds(getEmbed("❌ Пожалуйста, ответьте на все обязательные вопросы").build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }
            }
        }

        String nick = answers.getOrDefault("nick", "not_specified");

        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        String content = getInstance().getConfig().getString(CONFIG_TICKET_MESSAGE + ".content");
        EmbedBuilder embed = new EmbedBuild(CONFIG_TICKET_MESSAGE, event, event.getUser(), answers).getEmbedBuilder();

        if ((content == null || content.isEmpty()) && embed.isEmpty()) {
            event.getHook().sendMessageEmbeds(getEmbed("❌ Ошибка. В сообщение отсутствует содержание\nЗагляните в config" +
                            ".yml и добавьте содержание ticket-message для embed или content").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (content != null && !content.isEmpty()) {
            messageCreateBuilder.addContent(replacePlaceholder(content, event, event.getUser(), answers));
        }
        if (!embed.isEmpty()) messageCreateBuilder.addEmbeds(embed.build());

        String acceptText = getInstance().getConfig().getString(CONFIG_TICKET_MESSAGE + ".button.accept.text", "Принять");
        String acceptType = getInstance().getConfig().getString(CONFIG_TICKET_MESSAGE + ".button.accept.type",
                "SUCCESS");
        String acceptSymbol = getInstance().getConfig().getString(CONFIG_TICKET_MESSAGE + ".button.accept.symbol",
                null);

        String declineText = getInstance().getConfig().getString(CONFIG_TICKET_MESSAGE + ".button.decline.text", "Отклонить");
        String declineType = getInstance().getConfig().getString(CONFIG_TICKET_MESSAGE + ".button.decline.type",
                "SUCCESS");
        String declineSymbol = getInstance().getConfig().getString(CONFIG_TICKET_MESSAGE + ".button.decline.symbol",
                null);

        Button success = switch (acceptType) {
            case "SECONDARY" -> Button.secondary("accept_ticket", acceptText).withEmoji(EmojiParser.parseEmoji(acceptSymbol));
            case "PRIMARY" -> Button.primary("accept_ticket", acceptText).withEmoji(EmojiParser.parseEmoji(acceptSymbol));
            default -> Button.success("accept_ticket", acceptText).withEmoji(EmojiParser.parseEmoji(acceptSymbol));
        };

        Button danger = switch (declineType) {
            case "SECONDARY" -> Button.secondary("decline_ticket", declineText).withEmoji(EmojiParser.parseEmoji(declineSymbol));
            case "PRIMARY" -> Button.primary("decline_ticket", declineText).withEmoji(EmojiParser.parseEmoji(declineSymbol));
            default -> Button.danger("decline_ticket", declineText).withEmoji(EmojiParser.parseEmoji(declineSymbol));
        };

        messageCreateBuilder.addComponents(
                ActionRow.of(
                        success,
                        danger
                )
        );

        long categoryId = getInstance().getConfig().getLong(CONFIG_CATEGORY_ID, 0);
        Category category = event.getGuild().getCategoryById(categoryId);

        if (category == null) {
            event.getHook().sendMessageEmbeds(getEmbed("❌ Категория для тикетов не найдена. Обратитесь к администратору.").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        CompletableFuture.runAsync(() -> {
            ChannelAction<TextChannel> channelAction =
                    category.createTextChannel(replacePlaceholder(getInstance().getConfig().getString("discord" +
                                    ".ticket.name_format", "ticket-{user_name}"), null, event.getUser(), null))
                            .addPermissionOverride(event.getGuild().getPublicRole(), null, List.of(Permission.VIEW_CHANNEL))
                            .addPermissionOverride(event.getMember(), List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null);

            addStaffPermissions(channelAction, event.getGuild());

            channelAction.queue(textChannel -> {
                getDataBase().createTicket(textChannel.getId(), event.getUser().getId(), nick);

                textChannel.sendMessage(messageCreateBuilder.build()).queue();
                event.getHook().sendMessageEmbeds(getEmbed("✅ Ваше заявление на внесение в белый список " +
                                "отправлено!\nКанал: " + textChannel.getAsMention()).build())
                        .setEphemeral(true)
                        .queue();
            }, error -> {
                event.getHook().sendMessageEmbeds(getEmbed("❌ При создании канала произошла ошибка: " + error.getMessage()).build())
                        .setEphemeral(true)
                        .queue();
            });
        });
    }
}
