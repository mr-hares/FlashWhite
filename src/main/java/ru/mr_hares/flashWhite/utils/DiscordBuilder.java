package ru.mr_hares.flashWhite.utils;

import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiscordBuilder {
    private JavaPlugin plugin;

    public DiscordBuilder(JavaPlugin plugin) {
        List<String> msgs = List.of("discord/msg_welcome.yml", "discord/msg_log.yml", "discord/msg_ticket.yml",
                "discord/msg_verdict.yml");

        for (String msg: msgs) {
            File file = new File(plugin.getDataFolder(), msg);
            if (!file.exists()) plugin.saveResource(msg, false);
        }

        this.plugin = plugin;
    }

    public MessageCreateBuilder getWelcomeMessage() {
        File file = new File(plugin.getDataFolder(), "discord/msg_welcome.yml");
        if (!file.exists()) plugin.saveResource("discord/msg_welcome.yml", false);

        return getMCB(YamlConfiguration.loadConfiguration(file), null);
    }

    public MessageCreateBuilder getLogMessage(Map<String, String> replaces) {
        File file = new File(plugin.getDataFolder(), "discord/msg_log.yml");
        if (!file.exists()) plugin.saveResource("discord/msg_log.yml", false);

        return getMCB(YamlConfiguration.loadConfiguration(file), replaces);
    }

    public MessageCreateBuilder getVerdictMessage(Map<String, String> replaces) {
        File file = new File(plugin.getDataFolder(), "discord/msg_verdict.yml");
        if (!file.exists()) plugin.saveResource("discord/msg_verdict.yml", false);

        return getMCB(YamlConfiguration.loadConfiguration(file), replaces);
    }

    public MessageCreateBuilder getTicketMessage(Map<String, String> replaces) {
        File file = new File(plugin.getDataFolder(), "discord/msg_ticket.yml");
        if (!file.exists()) plugin.saveResource("discord/msg_ticket.yml", false);

        return getMCB(YamlConfiguration.loadConfiguration(file), replaces);
    }

    private @Nullable MessageCreateBuilder getMCB(YamlConfiguration yaml, @Nullable Map<String, String> replaces) {
        List<Map<?, ?>> components = yaml.getMapList("components");
        if (components.isEmpty()) return null;

        List<MessageTopLevelComponent> cmps = new ArrayList<>();

        MessageCreateBuilder mcb = new MessageCreateBuilder();
        mcb.useComponentsV2(true);
        for (Map<?, ?> component: components) {
            int type = (int) component.get("type");

            if (type == 10) {
                if (getTextDisplay(component, replaces) != null) cmps.add(getTextDisplay(component, replaces));
            } else if (type == 14) {
                cmps.add(getSeparator(component));
            } else if (type == 1) {
                if (getActionRow(component) != null) cmps.add(getActionRow(component));
            } else if (type == 17) {
                if (getContainer(component, replaces) != null) cmps.add(getContainer(component, replaces));
            } else if (type == 12) {
                if (getMediaGallery(component, replaces) != null) cmps.add(getMediaGallery(component, replaces));
            }
        }

        if (cmps.isEmpty()) return null;
        else mcb.addComponents(cmps);

        return mcb;
    }

    private TextDisplay getTextDisplay(Map<?, ?> component, Map<String, String> replaces) {
        String content = (String) component.getOrDefault("content", null);
        if (content == null) return null;

        return TextDisplay.of(replace(content, replaces));
    }

    private Separator getSeparator(Map<?, ?> component) {
        boolean divider = (boolean) (component.get("divider") == null ? true : component.get("divider"));
        return Separator.create(divider, Separator.Spacing.SMALL);
    }

    private Button getButton(Map<?, ?> component) {
        ButtonStyle style = switch ((int) component.get("style")) {
            case 4 -> ButtonStyle.DANGER;
            case 3 -> ButtonStyle.SUCCESS;
            case 2 -> ButtonStyle.SECONDARY;
            default -> ButtonStyle.PRIMARY;
        };

        String custom_id = switch ((String) component.get("custom_id")) {
            case "DECLINE" -> "decline_ticket";
            case "ACCEPT" -> "accept_ticket";
            case "CREATE" -> "create_ticket";
            default -> "unknow_button";
        };

        Emoji emoji = EmojiParser.parseEmoji((String) component.get("symbol"));
        String label = (String) component.get("label");

        if (label == null) return null;
        return Button.of(style, custom_id, label, emoji);
    }

    private ActionRow getActionRow(Map<?, ?> component) {
        List<Map<?, ?>> components = (List<Map<?, ?>>) component.get("components");
        List<Button> buttons = new ArrayList<>();

        for (Map<?, ?> cmp: components) {
            int type = (int) cmp.get("type");
            if (type == 2) {
                if (getButton(cmp) != null) buttons.add(getButton(cmp));
            }
        }

        if (buttons.isEmpty()) return null;
        return ActionRow.of(buttons);
    }

    private Container getContainer(Map<?, ?> component, Map<String, String> replaces) {
        List<Map<?, ?>> components = (List<Map<?, ?>>) component.get("components");
        if (components.isEmpty()) return null;

        List<ContainerChildComponent> cmps = new ArrayList<>();

        for (Map<?, ?> cmp: components) {
            int type = (int) cmp.get("type");

            if (type == 10) {
                if (getTextDisplay(cmp, replaces) != null) cmps.add(getTextDisplay(cmp, replaces));
            } else if (type == 14) {
                cmps.add(getSeparator(cmp));
            } else if (type == 1) {
                if (getActionRow(cmp) != null) cmps.add(getActionRow(cmp));
            } else if (type == 12) {
                if (getMediaGallery(cmp, replaces) != null) cmps.add(getMediaGallery(cmp, replaces));
            }
        }

        if (cmps.isEmpty()) return null;
        return Container.of(cmps);
    }

    private MediaGallery getMediaGallery(Map<?, ?> component, Map<String, String> replaces) {
        List<Map<?, ?>> components = (List<Map<?, ?>>) component.get("components");
        if (components.isEmpty()) return null;

        List<MediaGalleryItem> items = new ArrayList<>();

        for (Map<?, ?> cmp: components) {
            String url = replace((String) cmp.get("image"), replaces);
            if (validURL(url)) items.add(MediaGalleryItem.fromUrl(url));
        }

        if (items.isEmpty()) return null;
        return MediaGallery.of(items);
    }

    private boolean validURL(String text) {
        if (text == null) return false;
        return text.startsWith("http://") || text.startsWith("https://");
    }

    private String replace(String text, Map<String, String> replaces) {
        if (replaces == null) return text;

        for (String key: replaces.keySet()) {
            text = text.replace("{" + key + "}", replaces.get(key));
        }

        return text;
    }
}
