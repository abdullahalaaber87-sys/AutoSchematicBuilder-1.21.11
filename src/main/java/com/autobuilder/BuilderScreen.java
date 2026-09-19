package com.autobuilder;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class BuilderScreen extends Screen {

    public BuilderScreen() {
        super(Text.literal("Auto Schematic Builder"));
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Select Schematic"),
                button -> client.setScreen(new SchematicListScreen(this))
        ).dimensions(x, 70, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Start Build"),
                button -> {
                    if (BuilderManager.hasBuild()) {
                        AutoBuilderClient.state = AutoBuilderClient.BuildState.RUNNING;
                        AutoBuilderClient.message(client, "§aBuilder started");
                        close();
                    } else {
                        AutoBuilderClient.message(client, "§cSelect a schematic first");
                    }
                }
        ).dimensions(x, 100, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Pause / Resume"),
                button -> {
                    if (AutoBuilderClient.state == AutoBuilderClient.BuildState.RUNNING) {
                        AutoBuilderClient.state = AutoBuilderClient.BuildState.PAUSED;
                    } else if (AutoBuilderClient.state == AutoBuilderClient.BuildState.PAUSED) {
                        AutoBuilderClient.state = AutoBuilderClient.BuildState.RUNNING;
                    }
                }
        ).dimensions(x, 130, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Stop"),
                button -> {
                    BuilderManager.stop();
                    AutoBuilderClient.state = AutoBuilderClient.BuildState.STOPPED;
                }
        ).dimensions(x, 160, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer,
                "Auto Schematic Builder",
                width / 2,
                30,
                0xFFFFFF
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                "Status: " + AutoBuilderClient.state,
                width / 2,
                48,
                0xAAAAAA
        );

        String missing = BuilderManager.getMissingItem();

        if (missing != null) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    "Missing: " + missing,
                    width / 2,
                    200,
                    0xFF5555
            );
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
