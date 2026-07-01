package ctx.mr_hares.flashWhite.discord.listener;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ctx.mr_hares.flashWhite.FlashWhite.*;

public class ButtonInteraction extends ListenerAdapter {
    private static final String CONFIG_QUESTIONS = "discord.questions";
    private static final String CONFIG_ROLE_STAFF = "discord.role_staff";

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
                    event.replyEmbeds(getEmbed("❌ У вас уже имеется активный тикет").build())
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

            boolean isAccept = buttonId.equals("accept_ticket");
            String verdict = isAccept ? "Принять" : "Отклонить";

            EmbedBuilder verdictMessage = new EmbedBuilder();
            verdictMessage.setTitle("Решение по тикету");
            verdictMessage.addField("Модератор", event.getUser().getAsMention(), true);
            verdictMessage.addField("Вердикт", verdict, true);
            verdictMessage.addField("Дата закрытия", String.format("<t:%d:F>", Instant.now().getEpochSecond()), false);
            verdictMessage.setFooter("Спасибо за обращение");

            if (isAccept && ticket.length > 1 && ticket[1] != null && !ticket[1].equals("not_specified")) {
                getDataBase().addPlayer("not", ticket[1], "[DS] " + event.getUser().getName());
                sendConsole("(FlashWhite) Игрок " + ticket[1] + " добавлен в белый список модератором " + event.getUser().getName() + " с Discord");
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
                    EmbedBuilder logMessage = new EmbedBuilder();
                    logMessage.setTitle("Решение по тикету");
                    logMessage.addField("Открыл", user.getAsMention(), true);
                    logMessage.addField("Закрыл", event.getMember().getAsMention(), true);
                    if (!ticket[1].equals("not_specified")) {
                        logMessage.addField("Указанный ник", ticket[1], false);
                    }
                    logMessage.addField("Вердикт", verdict, false);

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
