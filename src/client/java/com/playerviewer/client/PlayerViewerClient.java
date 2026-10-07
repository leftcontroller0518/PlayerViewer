package com.playerviewer.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.playerviewer.client.config.PlayerViewerConfig;
import com.playerviewer.client.hud.PlayerViewerHudElement;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PlayerViewerClient implements ClientModInitializer {
    public static final String MOD_ID = "playerviewer";

    private static KeyMapping toggleKey;
    private static KeyMapping toggleDurabilityKey;

    @Override
    public void onInitializeClient() {
        // キーバインドの登録 (Vキーで表示切替)
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.playerviewer.toggle",
            InputConstants.KEY_V,
            KeyMapping.Category.MISC
        ));

        // 耐久値表示切替キー (デフォルト未割り当て)
        toggleDurabilityKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.playerviewer.toggle_durability",
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.MISC
        ));

        // HUD要素の登録
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
            new PlayerViewerHudElement()
        );

        // キー入力監視
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            PlayerViewerConfig config = PlayerViewerConfig.getInstance();

            while (toggleKey.consumeClick()) {
                config.enabled = !config.enabled;
                config.save();
                client.player.sendOverlayMessage(
                    Component.literal("§6[PlayerViewer] §f表示: " + (config.enabled ? "§aON" : "§cOFF"))
                );
            }

            while (toggleDurabilityKey.consumeClick()) {
                config.showDurability = !config.showDurability;
                config.save();
                client.player.sendOverlayMessage(
                    Component.literal("§6[PlayerViewer] §f耐久値表示: " + (config.showDurability ? "§aON" : "§cOFF"))
                );
            }
        });
    }
}
