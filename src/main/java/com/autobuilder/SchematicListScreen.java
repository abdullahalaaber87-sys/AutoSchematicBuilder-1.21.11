package com.autobuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class SchematicListScreen extends Screen {

    private final Screen parent;
    private int page;

    public SchematicListScreen(Screen parent) {
        super(Text.literal("Select Schematic"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        File minecraftFolder = MinecraftClient.getInstance().runDirectory;

        File folder = new File(minecraftFolder, "schematics");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        File[] files = folder.listFiles(file ->
                file.isFile() &&
                file.getName().toLowerCase().endsWith(".litematic")
        );

        int perPage = Math.max(1, (height - 105) / 24);
        int y = 45;

        if (files != null && files.length > 0) {
            Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

            page = Math.min(page, (files.length - 1) / perPage);
            for (int i = page * perPage; i < Math.min(files.length, (page + 1) * perPage); i++) {
                File file = files[i];

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
        } else {
            System.out.println(
                    "[AutoSchematicBuilder] No .litematic files found in: "
                            + folder.getAbsolutePath()
            );
        }

        int count = files == null ? 0 : files.length;
        var previous = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page--;
            clearAndInit();
        }).dimensions(width / 2 - 150, height - 55, 35, 20).build());
        previous.active = page > 0;
        var next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            clearAndInit();
        }).dimensions(width / 2 + 115, height - 55, 35, 20).build());
        next.active = (page + 1) * perPage < count;

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
        if (files == null || files.length == 0) context.drawCenteredTextWithShadow(
                textRenderer, "No files in .minecraft/schematics", width / 2, 55, 0xFFFFFF55);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
