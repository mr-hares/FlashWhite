package ctx.mr_hares.flashWhite.discord.listener;

import ctx.mr_hares.flashWhite.utils.EmbedBuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ctx.mr_hares.flashWhite.FlashWhite.*;

public class ButtonInteraction extends ListenerAdapter {
    private static final String CONFIG_QUESTIONS = "discord.questions";
    private static final String CONFIG_ROLE_STAFF = "discord.role_staff";
    private static final String CONFIG_VERDICT_MESSAGE = "discord.verdict-message";
    private static final String CONFIG_LOG_MESSAGE = "discord.log-message";

    private Map<String, String> getUserPlaceholders(String prefix, User user) {
        Map<String, String> placeholders = new HashMap<>();

        placeholders.put("{" + prefix + "_mention}", user.getAsMention());
        placeholders.put("{" + prefix + "_id}", user.getId());
        placeholders.put("{" + prefix + "_name}", user.getName());
        placeholders.put("{" + prefix + "_tag}", user.getAsTag());
        placeholders.put("{" + prefix + "_avatar}", user.getAvatarUrl() != null ? user.getAvatarUrl() : user.getDefaultAvatarUrl());

        return placeholders;
    }

    private boolean hasPermission(Member member) {
        if (member == null) return false;
        if (member.hasPermission(Permission.ADMINISTRATOR)) return true;

        List<?> roleStaff = getInstance().getConfig().getList(CONFIG_ROLE_STAFF);
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

    private Modal createModalFromConfig(ConfigurationSection questions) {
        List<Label> textInputs = new ArrayList<>();

        for (String key : questions.getKeys(false)) {
            ConfigurationSection question = questions.getConfigurationSection(key);
            if (question != null) {
                String title = question.getString("title");
                if (title != null && !title.isEmpty()) {
                    String placeholder = question.getString("placeholder", "");
                    int minLength = question.getInt("min_length", 1);
                    int maxLength = question.getInt("max_length", 4000);
                    boolean required = question.getBoolean("required", true);

                    String typeStr = question.getString("type", "SHORT");
                    TextInputStyle style = typeStr.equalsIgnoreCase("PARAGRAPH")
                            ? TextInputStyle.PARAGRAPH
                            : TextInputStyle.SHORT;

                    TextInput.Builder textInput = TextInput.create(key, style)
                            .setMinLength(minLength)
                            .setMaxLength(maxLength)
                            .setRequired(required);

                    if (placeholder != null && !placeholder.isEmpty()) {
                        textInput.setPlaceholder(placeholder);
                    }

                    textInputs.add(Label.of(title, textInput.build()));
                }
            }
        }

        Modal.Builder modalBuilder = Modal.create("whitelist-questions", getInstance().getConfig().getString("discord" +
                ".modal-title", "Форма заявления"));
        modalBuilder.addComponents(textInputs);
        return modalBuilder.build();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getCustomId();

        if (buttonId.equals("open_ticket")) {
            try {
                String[] existingTicket = getDataBase().getTicket(event.getUser().getIdLong());
                if (existingTicket != null) {
                    event.replyEmbeds(getEmbed(getLocale().getString("discord.already-create-ticket")).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                ConfigurationSection questions = getInstance().getConfig().getConfigurationSection(CONFIG_QUESTIONS);
                if (questions == null || questions.getKeys(false).isEmpty()) {
                    event.replyEmbeds(getEmbed("❌ Вопросы для анкеты не настроены. Обратитесь к администратору.").build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                CompletableFuture.runAsync(() -> {
                    event.replyModal(createModalFromConfig(questions)).queue();
                });

                return;
            } catch (Exception e) {
                event.replyEmbeds(getEmbed("❌ Непредвиденная ошибка. Попробуйте ещё раз").build())
                        .setEphemeral(true)
                        .queue();
                return;
            }
        }

        if (buttonId.equals("accept_ticket") || buttonId.equals("decline_ticket")) {
            event.deferReply(true).queue();

            if (!hasPermission(event.getMember())) {
                event.getHook().sendMessageEmbeds(getEmbed("❌ У вас нет прав для этого действия").build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            String[] ticket = getDataBase().getTicket(event.getChannelId());
            if (ticket == null) {
                event.getHook().sendMessageEmbeds(getEmbed("❌ Этот канал не является тикетом").build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            String acceptText = getInstance().getConfig().getString("discord.ticket-message.button.accept.text", "Принять");
            String declineText = getInstance().getConfig().getString("discord.ticket-message.button.decline.text", "Отклонить");

            boolean isAccept = buttonId.equals("accept_ticket");
            String verdict = isAccept ? acceptText : declineText;

            Map<String, String> plList = getUserPlaceholders("moder", event.getUser());
            plList.put("{date}", String.format("<t:%d:F>", Instant.now().getEpochSecond()));
            plList.put("{verdict}", verdict);

            EmbedBuilder verdictMessage = new EmbedBuild(CONFIG_VERDICT_MESSAGE, null, plList, null).getEmbedBuilder();

            if (isAccept && ticket.length > 1 && ticket[1] != null && !ticket[1].equals("not_specified")) {
                getDataBase().addPlayer("not", ticket[1], "[DS] " + event.getUser().getName());
                sendConsole("(FlashWhite) Player" + ticket[1] + " whitelisted by " + event.getUser().getName() + " (Discord)");
            }

            getJda().retrieveUserById(ticket[0]).queue(user -> {
                Role grant_role = event.getGuild().getRoleById(getInstance().getConfig().getString("discord" +
                        ".grant_role", "123456789012345678"));
                if (grant_role != null && isAccept) {
                    try {
                        event.getGuild().addRoleToMember(user, grant_role).reason("Ticket by FlashWhite").queue();
                    } catch (Exception e) {
                        event.getHook().sendMessage("Ошибка при выдаче роли. Проверьте, находится ли бот выше " +
                                "указанной роли в конфиге").setEphemeral(true).queue();
                    }
                }

                if (logChannel != null) {
                    Map<String, String> placeholders = getUserPlaceholders("user", user);
                    placeholders.putAll(plList);
                    placeholders.put("{nick}", ticket[1]);

                    EmbedBuilder logMessage = new EmbedBuild(CONFIG_LOG_MESSAGE, null, placeholders, null).getEmbedBuilder();
                    logChannel.sendMessageEmbeds(logMessage.build()).queue();
                }

                user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(verdictMessage.build()).queue());
            });

            getDataBase().removeTicket(event.getChannelId());
            event.getHook().sendMessageEmbeds(getEmbed("Вы приняли решение по данному тикету. Канал будет удалён через 5 " +
                    "секунд.").build()).setEphemeral(true).queue();
            event.getChannel().delete().queueAfter(5, TimeUnit.SECONDS);
        }
    }
}
