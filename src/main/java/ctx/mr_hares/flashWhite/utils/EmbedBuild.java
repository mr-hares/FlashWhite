package ctx.mr_hares.flashWhite.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

import java.awt.*;
import java.time.Instant;
import java.util.Map;

import static ctx.mr_hares.flashWhite.FlashWhite.*;

public class EmbedBuild implements Listener {
    private EmbedBuilder embedBuilder;

    private String validURL(String text) {
        if (text == null) return null;

        return text.startsWith("http://") || text.startsWith("https://") ? text : null;
    }

    public EmbedBuild(String configPath, ModalInteractionEvent event, User user,
                                Map<String, String> answers) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        String title = getInstance().getConfig().getString(configPath + ".embed.title");
        if (title != null && !title.isEmpty()) {
            embedBuilder.setTitle(replacePlaceholder(title, event, user, answers));
        }

        String authorName = getInstance().getConfig().getString(configPath + ".embed.author.name", null);
        String authorUrl = getInstance().getConfig().getString(configPath + ".embed.author.url", null);
        String authorIcon = getInstance().getConfig().getString(configPath + ".embed.author.icon_url", null);

        if (authorName != null && !authorName.isEmpty()) {
            String processedAuthorName = replacePlaceholder(authorName, event, user, answers);
            embedBuilder.setAuthor(processedAuthorName, validURL(authorUrl), validURL(replacePlaceholder(authorIcon, event, user, answers)));
        }

        String description = getInstance().getConfig().getString(configPath + ".embed.description");
        if (description != null && !description.isEmpty()) {
            embedBuilder.setDescription(replacePlaceholder(description, event, user, answers));
        }

        ConfigurationSection fields = getInstance().getConfig().getConfigurationSection(configPath + ".embed.fields");
        if (fields != null) {
            for (String key : fields.getKeys(false)) {
                String name = fields.getString(key + ".name");
                String value = fields.getString(key + ".value");
                boolean inline = fields.getBoolean(key + ".inline", false);

                if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
                    embedBuilder.addField(
                            replacePlaceholder(name, event, user, answers),
                            replacePlaceholder(value, event, user, answers),
                            inline
                    );
                }
            }
        }

        String imageUrl = getInstance().getConfig().getString(configPath + ".embed.image_url", null);
        String thumbnailUrl = getInstance().getConfig().getString(configPath + ".embed.thumbnail_url", null);

        if (imageUrl != null && !imageUrl.isEmpty()) embedBuilder.setImage(validURL(imageUrl));
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) embedBuilder.setThumbnail(validURL(thumbnailUrl));

        boolean timestamp = getInstance().getConfig().getBoolean(configPath + ".embed.timestamp", false);
        if (timestamp) embedBuilder.setTimestamp(Instant.now());

        String color = getInstance().getConfig().getString(configPath + ".embed.color", "#000000");
        if (!color.equals("#000000")) embedBuilder.setColor(Color.getColor(color));

        this.embedBuilder = embedBuilder;
    }

    public EmbedBuilder getEmbedBuilder() {
        return embedBuilder;
    }
}
