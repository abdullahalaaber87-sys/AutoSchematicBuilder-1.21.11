package com.autobuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.util.Arrays;

public class SchematicListScreen extends Screen {

    private final Screen parent;

    public SchematicListScreen(Screen parent) {
        super(Text.literal("Select Schematic"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        File folder = new File(
                MinecraftClient.getInstance().runDirectory,
                "schematics"
        );

        if (!folder.exists()) {
            folder.mkdirs();
        }

        File[] files = folder.listFiles(file ->
                file.isFile() &&
                file.getName().toLowerCase().endsWith(".litematic")
        );

        if (files != null) {
            Arrays.sort(files);

            int y = 45;

            for (File file : files) {
                if (y > height - 60) {
                    break;
                }

                addDrawableChild(ButtonWidget.builder(
                        Text.literal(file.getName()),
                        button -> {
                            BuilderManager.select(file);
                            client.setScreen(parent);
                        }
                ).dimensions(
                        width / 2 - 150,
                        y,
                        300,
                        20
                ).build());

                y += 24;
            }
        }

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"),
                button -> client.setScreen(parent)
        ).dimensions(
                width / 2 - 50,
                height - 30,
                100,
                20
        ).build());
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer,
                "Select a .litematic",
                width / 2,
                20,
                0xFFFFFF
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
