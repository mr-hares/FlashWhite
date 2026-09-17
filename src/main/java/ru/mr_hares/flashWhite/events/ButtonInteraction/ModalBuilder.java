package ru.mr_hares.flashWhite.events.ButtonInteraction;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.bukkit.configuration.ConfigurationSection;
import ru.mr_hares.flashWhite.FlashWhite;

import java.util.ArrayList;
import java.util.List;

public class ModalBuilder {
    private Modal modal = null;

    public ModalBuilder(FlashWhite plugin) {
        List<Label> textInputs = new ArrayList<>();
        ConfigurationSection questions = plugin.getConfig().getConfigurationSection("application_form.questions");
        if (questions == null) return;

        for (String key : questions.getKeys(false)) {
            ConfigurationSection question = questions.getConfigurationSection(key);
            if (question != null) {
                String label = question.getString("label");
                if (label != null && !label.isEmpty()) {
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

                    textInputs.add(Label.of(label, textInput.build()));
                }
            }
        }

        if (textInputs.isEmpty()) return;

        Modal.Builder modalBuilder = Modal.create("whitelist-questions",
                plugin.getConfig().getString("interface.modal_title", "Верификация аккаунта"));
        modalBuilder.addComponents(textInputs);
        this.modal = modalBuilder.build();
    }

    public Modal getModal() {
        return modal;
    }
}
