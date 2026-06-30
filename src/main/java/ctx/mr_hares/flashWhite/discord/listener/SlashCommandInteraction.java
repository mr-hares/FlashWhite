package ctx.mr_hares.flashWhite.discord.listener;

import ctx.mr_hares.flashWhite.utils.EmbedBuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ctx.mr_hares.flashWhite.FlashWhite.*;

public class SlashCommandInteraction extends ListenerAdapter {
    private static final String CONFIG_CATEGORY_ID = "discord.category_id";
    private static final String CONFIG_ROLE_STAFF = "discord.role_staff";
    private static final String CONFIG_INFO_MESSAGE = "discord.info-message";

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

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("setup")) return;

        event.deferReply(true).queue();

        if (!hasPermission(event.getMember())) {
            event.getHook().sendMessage(getEmbed("❌ У вас нет прав для использования этой команды").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        long categoryId = getInstance().getConfig().getLong(CONFIG_CATEGORY_ID, 0);
        if (categoryId == 0 || event.getGuild().getCategoryById(categoryId) == null) {
            event.getHook().sendMessage(getEmbed("❌ Установите ID категории в config.yml (category_id), в которой " +
                            "будут создаваться тикеты").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
        String content = getInstance().getConfig().getString(CONFIG_INFO_MESSAGE + ".content");
        EmbedBuilder embed = new EmbedBuild(CONFIG_INFO_MESSAGE, null, null, null).getEmbedBuilder();

        if ((content == null || content.isEmpty()) && embed.isEmpty()) {
            event.getHook().sendMessage(getEmbed("❌ Ошибка. В сообщение отсутствует содержание\nЗагляните в config" +
                            ".yml и добавьте содержание info-message для embed или content").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (content != null && !content.isEmpty()) messageCreateBuilder.addContent(content);
        if (!embed.isEmpty()) messageCreateBuilder.addEmbeds(embed.build());

        CompletableFuture.runAsync(() -> {
            String buttonText = getInstance().getConfig().getString(CONFIG_INFO_MESSAGE + ".button.text", "Подать заявку в белый список");
            messageCreateBuilder.addComponents(ActionRow.of(Button.secondary("open_ticket", buttonText)));

            event.getChannel().sendMessage(messageCreateBuilder.build())
                    .queue(success -> {
                        event.getHook().sendMessage(getEmbed("✅ Информационное сообщение успешно отправлено").build())
                                .setEphemeral(true)
                                .queue();
                    }, error -> {
                        event.getHook().sendMessage(getEmbed("❌ При отправке информационного сообщения произошла " +
                                        "ошибка\n\n" + error.getMessage()).build())
                                .setEphemeral(true)
                                .queue();
                    });
        });
    }
}
