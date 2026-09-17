package ru.mr_hares.flashWhite.utils;

import com.google.common.base.Charsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.beans.JavaBean;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MessageManager {
    private final JavaPlugin plugin;
    private static YamlConfiguration yaml;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);

        yaml = YamlConfiguration.loadConfiguration(file);

        final InputStream defYamlStream = plugin.getResource("config.yml");
        if (defYamlStream == null) {
            return;
        }

        yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defYamlStream, Charsets.UTF_8)));
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);

        yaml = YamlConfiguration.loadConfiguration(file);

        final InputStream defYamlStream = plugin.getResource("config.yml");
        if (defYamlStream == null) {
            return;
        }

        yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defYamlStream, Charsets.UTF_8)));
    }

    public String getString(String path) {
        try (InputStream defMessage = plugin.getResource("messages.yml")) {
            assert defMessage != null;
            try (InputStreamReader reader = new InputStreamReader(defMessage, StandardCharsets.UTF_8)) {

                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                return yaml.getString(path, defaultConfig.getString(path));
            }
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getStringList(String path) {
        try (InputStream defMessage = plugin.getResource("messages.yml")) {
            assert defMessage != null;
            try (InputStreamReader reader = new InputStreamReader(defMessage, StandardCharsets.UTF_8)) {

                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                List<String> result = yaml.getStringList(path);
                return result.isEmpty() ? defaultConfig.getStringList(path) : result;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
