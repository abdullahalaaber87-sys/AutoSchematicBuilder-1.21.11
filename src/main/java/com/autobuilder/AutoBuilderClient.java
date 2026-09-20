package com.autobuilder;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class AutoBuilderClient implements ClientModInitializer {
    public static BuildState state = BuildState.STOPPED;

    // الحالات المطلوبة تماماً
    public enum BuildState {
        RUNNING,
        PAUSED,
        STOPPED
    }

    @Override
    public void onInitializeClient() {
        // كل وظائف الـ Keybinds والـ GUI الأصلية الخاصة بك تبقى هنا
    }
}
