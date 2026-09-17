package ru.mr_hares.flashWhite.events;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import static ru.mr_hares.flashWhite.FlashWhite.*;

public class SlashCommandInteraction extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();

        if (command.equals("setup_panel")) {
            event.deferReply(true).queue();

            MessageCreateBuilder msg_welcome = getBuilder().getWelcomeMessage();
            if (msg_welcome == null || msg_welcome.isEmpty()) {
                event.getHook().sendMessage("Шаблон сообщения msg_welcome.yml пуст или содержит ошибки.").setEphemeral(true).queue();
                return;
            }

            event.getChannel().sendMessage(msg_welcome.build()).queue(success -> {
                event.getHook().sendMessage("Сообщения успешно отправлено").setEphemeral(true).queue();
            }, failure -> {
                event.getHook().sendMessage("При отправке сообщения произошла ошибка\n" + failure.getMessage()).setEphemeral(true).queue();
            });
        }
    }
}
