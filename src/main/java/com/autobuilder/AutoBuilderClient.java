package com.autobuilder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoBuilderClient implements ClientModInitializer {

    public static KeyBinding GUI_KEY;
    public static KeyBinding CONTROL_KEY;

    public static BuildState state = BuildState.STOPPED;

    @Override
    public void onInitializeClient() {

        GUI_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoschematicbuilder.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KeyBinding.Category.MISC
        ));

        CONTROL_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoschematicbuilder.control",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (GUI_KEY.wasPressed()) {
                client.setScreen(new BuilderScreen());
            }

            while (CONTROL_KEY.wasPressed()) {
                handleControlKey(client);
            }

            if (state == BuildState.RUNNING) {
                BuilderManager.tick(client);
            }
        });
    }

    private static void handleControlKey(MinecraftClient client) {

        switch (state) {

            case RUNNING -> {
                state = BuildState.PAUSED;
                message(client, "§eAuto Builder paused");
            }

            case PAUSED -> {
                state = BuildState.STOPPED;
                BuilderManager.stop();
                message(client, "§cAuto Builder stopped");
            }

            case STOPPED -> {
                if (BuilderManager.hasBuild()) {
                    state = BuildState.RUNNING;
                    message(client, "§aAuto Builder resumed");
                } else {
                    message(client, "§cPress H and select a schematic first");
                }
            }
        }
    }

    public static void message(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }

    public enum BuildState {
        RUNNING,
        PAUSED,
        STOPPED
    }
}
