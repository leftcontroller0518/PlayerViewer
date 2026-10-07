package com.playerviewer.client.hud;

import com.playerviewer.client.config.PlayerViewerConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PlayerViewerHudElement implements HudElement {

    // 定数・キャッシュ
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    // 耐久値テキスト色
    private static final int COLOR_DURABILITY_HIGH = 0xFF55FF55;   // 緑 (>60%)
    private static final int COLOR_DURABILITY_MEDIUM = 0xFFFFFF55; // 黄 (>30%)
    private static final int COLOR_DURABILITY_LOW = 0xFFFF5555;    // 赤 (<=30%)

    // インベントリ風スロット枠色
    private static final int COLOR_SLOT_BG = 0x88000000;           // 半透明の暗い背景
    private static final int COLOR_SLOT_BORDER_DARK = 0xCC373737;  // 凹みの上/左（暗い影）
    private static final int COLOR_SLOT_BORDER_LIGHT = 0xCCFFFFFF; // 凹みの下/右（ハイライト）
    private static final int COLOR_PANEL_BG = 0x55000000;          // 全体背景パネル

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        PlayerViewerConfig config = PlayerViewerConfig.getInstance();
        if (!config.enabled) {
            return;
        }

        // F1非表示チェック
        if (mc.gui.hud.isHidden()) {
            return;
        }

        // GUI画面チェック (チャット以外のScreenが開いているときは非表示)
        Screen currentScreen = mc.gui.screen();
        if (currentScreen != null && !(currentScreen instanceof ChatScreen)) {
            return;
        }

        // スペクテイターモード除外
        if (player.isSpectator()) {
            return;
        }

        // アクション中のみ表示オプション
        if (config.onlyWhenActive && !isPlayerActive(player)) {
            return;
        }

        // 基準座標（プレイヤー人形の中心）
        final int screenWidth = extractor.guiWidth();
        final int centerX = screenWidth - config.offsetX;
        final int centerY = config.offsetY;

        // 全体背景パネル（オプション）
        if (config.showBackground) {
            extractor.fill(centerX - 60, centerY - 56, centerX + 30, centerY + 52, COLOR_PANEL_BG);
        }

        // 1. プレイヤーモデル（ペーパードール）の描画
        renderPlayerModel(extractor, mc, player, config, centerX, centerY);

        // 2. 装備スロットおよび耐久値の描画
        if (config.showDurability) {
            renderEquipmentAndDurability(extractor, mc.font, player, centerX, centerY);
        }
    }

    /**
     * 統合版スタイルのペーパードール描画
     */
    private void renderPlayerModel(GuiGraphicsExtractor extractor, Minecraft mc, LocalPlayer player,
                                   PlayerViewerConfig config, int centerX, int centerY) {
        final int boxHalfWidth = 28;
        final int x1 = centerX - boxHalfWidth;
        final int y1 = centerY - 52;
        final int x2 = centerX + boxHalfWidth;
        final int y2 = centerY + 20;

        float mouseX;
        float mouseY;

        if (config.followLook) {
            mouseX = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
            mouseY = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
        } else {
            // 統合版ペーパードール風の斜め前（約-25度）を向く自然なアングル
            mouseX = centerX - 25.0f;
            mouseY = (y1 + y2) * 0.5f;
        }

        InventoryScreen.extractEntityInInventoryFollowsMouse(
            extractor,
            x1, y1, x2, y2,
            config.scale,
            0.0625f,
            mouseX,
            mouseY,
            player
        );
    }

    /**
     * 装備品（防具は左側、手持ちアイテムは下側）をインベントリ風スロットで描画
     */
    private void renderEquipmentAndDurability(GuiGraphicsExtractor extractor, Font font, LocalPlayer player,
                                              int centerX, int centerY) {
        // [防具]: プレイヤーの左側に上から順（頭、胴、脚、足）に縦配置
        final int armorX = centerX - 54;
        int currentArmorY = centerY - 50;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            renderInventorySlot(extractor, font, stack, armorX, currentArmorY, true);
            currentArmorY += 20; // スロット高さ18px + 間隔2px
        }

        // [手持ちアイテム]: プレイヤーの人形の下に横並びで配置（左: オフハンド, 右: メインハンド）
        final int handY = centerY + 24;
        final int offhandX = centerX - 20;
        final int mainhandX = centerX + 2;

        renderInventorySlot(extractor, font, player.getItemBySlot(EquipmentSlot.OFFHAND), offhandX, handY, false);
        renderInventorySlot(extractor, font, player.getItemBySlot(EquipmentSlot.MAINHAND), mainhandX, handY, false);
    }

    /**
     * インベントリスロット風の枠（18x18px）とアイテム・耐久度を描画
     *
     * @param showTextOnLeft 耐久値数値をスロットの左側に表示するか（防具用: true, 手持ち用: false）
     */
    private void renderInventorySlot(GuiGraphicsExtractor extractor, Font font, ItemStack stack,
                                    int x, int y, boolean showTextOnLeft) {
        // インベントリ風立体スロット枠の描画
        extractor.fill(x, y, x + 18, y + 18, COLOR_SLOT_BG);
        // 上・左（暗い枠）
        extractor.fill(x, y, x + 18, y + 1, COLOR_SLOT_BORDER_DARK);
        extractor.fill(x, y, x + 1, y + 18, COLOR_SLOT_BORDER_DARK);
        // 下・右（明るいハイライト枠）
        extractor.fill(x, y + 17, x + 18, y + 18, COLOR_SLOT_BORDER_LIGHT);
        extractor.fill(x + 17, y, x + 18, y + 18, COLOR_SLOT_BORDER_LIGHT);

        if (stack.isEmpty()) {
            return;
        }

        // アイテムアイコン（内側 16x16）
        final int itemX = x + 1;
        final int itemY = y + 1;
        extractor.item(stack, itemX, itemY);
        extractor.itemDecorations(font, stack, itemX, itemY);

        // 耐久値数値テキスト
        if (stack.isDamageableItem()) {
            final int maxDamage = stack.getMaxDamage();
            final int damage = stack.getDamageValue();
            final int current = maxDamage - damage;
            final float ratio = (float) current / maxDamage;

            final int textColor;
            if (ratio > 0.6f) {
                textColor = COLOR_DURABILITY_HIGH;
            } else if (ratio > 0.3f) {
                textColor = COLOR_DURABILITY_MEDIUM;
            } else {
                textColor = COLOR_DURABILITY_LOW;
            }

            final String text = Integer.toString(current);
            final int textWidth = font.width(text);

            if (showTextOnLeft) {
                // スロットの左側に表示（防具用）
                extractor.text(font, text, x - textWidth - 3, y + 5, textColor, true);
            } else {
                // スロットの直下に中央揃えで表示（手持ち用）
                extractor.text(font, text, x + 9 - (textWidth / 2), y + 19, textColor, true);
            }
        }
    }

    /**
     * プレイヤーがアクション中（動いている・特殊状態）かどうかを判定
     */
    private boolean isPlayerActive(LocalPlayer player) {
        return player.isSprinting()
            || player.isShiftKeyDown()
            || player.isSwimming()
            || player.isFallFlying()
            || player.isSleeping()
            || player.isUsingItem()
            || (player.getDeltaMovement().horizontalDistanceSqr() > 1.0e-4);
    }
}
