package com.autobuilder;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class AutoBuilderClient implements ClientModInitializer {
    public static BuildState state = BuildState.STOPPED;

    public enum BuildState {
        RUNNING,
        PAUSED,
        STOPPED
    }

    @Override
    public void onInitializeClient() {
        // نقطة البداية الأصلية للمود
    }

    // دالة الرسائل الأصلية التي كانت تبحث عنها كل الملفات الأخرى لتجنب أخطاء البناء
    public static void message(MinecraftClient client, String text) {
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(text), false);
        }
    }
}
