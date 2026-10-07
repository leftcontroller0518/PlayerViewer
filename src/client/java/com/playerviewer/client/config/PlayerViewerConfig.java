package com.playerviewer.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PlayerViewerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "playerviewer.json");

    public boolean enabled = true;
    public boolean showDurability = true;
    public boolean onlyWhenActive = false;
    public int scale = 25;
    public int offsetX = 48;
    public int offsetY = 55;
    public boolean showBackground = false;
    public boolean followLook = false;

    private static PlayerViewerConfig INSTANCE;

    public static PlayerViewerConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static PlayerViewerConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                PlayerViewerConfig config = GSON.fromJson(reader, PlayerViewerConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PlayerViewerConfig config = new PlayerViewerConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
