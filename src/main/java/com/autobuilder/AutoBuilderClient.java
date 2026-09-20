package com.autobuilder;

import net.fabricmc.api.ClientModInitializer;
// أضف أي imports أصلية أخرى هنا إذا احتجتها

public class AutoBuilderClient implements ClientModInitializer {
    public static BuildState state = BuildState.STOPPED;

    @Override
    public void onInitializeClient() {
        // ضع كود التهيئة الأصلي الخاص بك هنا
    }

    public enum BuildState {
        RUNNING,
        PAUSED,
        STOPPED
    }
}
