package com.autobuilder;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class MaterialsScreen extends Screen {
    private final Screen parent;
    private List<Row> rows = List.of();
    private int scroll = 0;
    private String error;

    public MaterialsScreen(Screen parent) {
        super(Text.literal("Materials"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        try {
            Map<String, Long> needed = BuilderManager.getMaterials();

            List<Row> list = new ArrayList<>();

            for (var e : needed.entrySet()) {
                long have = countInventory(e.getKey());
                list.add(new Row(
                        pretty(e.getKey()),
                        e.getValue(),
                        have
                ));
            }

            list.sort(
                    Comparator.comparingLong(Row::missing)
                            .reversed()
                            .thenComparing(Row::name)
            );

            rows = list;

        } catch (Exception e) {
            error = e.getMessage();

            if (error == null || error.isBlank()) {
                error = e.getClass().getSimpleName();
            }

            e.printStackTrace();
        }

        addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Back"),
                        b -> client.setScreen(parent)
                ).dimensions(width / 2 - 85, height - 32, 170, 20).build()
        );
    }

    private long countInventory(String id) {
        if (client == null || client.player == null) {
            return 0;
        }

        Identifier identifier = Identifier.tryParse(id);

        if (identifier == null) {
            return 0;
        }

        var item = Registries.ITEM.get(identifier);
        long count = 0;

        for (var stack : client.player.getInventory().getMainStacks()) {
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private String pretty(String id) {
        String name = id;

        int colon = name.indexOf(':');

        if (colon >= 0) {
            name = name.substring(colon + 1);
        }

        name = name.replace('_', ' ');

        StringBuilder result = new StringBuilder();
        boolean upper = true;

        for (char c : name.toCharArray()) {
            if (upper && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                upper = false;
            } else {
                result.append(c);
            }

            if (c == ' ') {
                upper = true;
            }
        }

        return result.toString();
    }

    @Override
    public void render(
            DrawContext c,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.render(c, mouseX, mouseY, delta);

        c.drawCenteredTextWithShadow(
                textRenderer,
                "Materials",
                width / 2,
                15,
                0xFFFFFFFF
        );

        if (error != null) {
            c.drawCenteredTextWithShadow(
                    textRenderer,
                    "Could not read schematic: " + error,
                    width / 2,
                    50,
                    0xFFFF5555
            );

            return;
        }

        int startY = 42;
        int rowHeight = 14;

        c.drawTextWithShadow(
                textRenderer,
                "Material",
                width / 2 - 190,
                28,
                0xFFFFFFFF
        );

        c.drawTextWithShadow(
                textRenderer,
                "Required",
                width / 2 + 20,
                28,
                0xFFFFFFFF
        );

        c.drawTextWithShadow(
                textRenderer,
                "Have",
                width / 2 + 90,
                28,
                0xFFFFFFFF
        );

        c.drawTextWithShadow(
                textRenderer,
                "Missing",
                width / 2 + 150,
                28,
                0xFFFFFFFF
        );

        int maxVisible = Math.max(
                1,
                (height - startY - 50) / rowHeight
        );

        int end = Math.min(
                rows.size(),
                scroll + maxVisible
        );

        int y = startY;

        for (int i = scroll; i < end; i++) {
            Row r = rows.get(i);

            c.drawTextWithShadow(
                    textRenderer,
                    r.name(),
                    width / 2 - 190,
                    y,
                    0xFFFFFFFF
            );

            c.drawTextWithShadow(
                    textRenderer,
                    Long.toString(r.required()),
                    width / 2 + 20,
                    y,
                    0xFFFFFFFF
            );

            c.drawTextWithShadow(
                    textRenderer,
                    Long.toString(r.have()),
                    width / 2 + 90,
                    y,
                    0xFFAAAAAA
            );

            c.drawTextWithShadow(
                    textRenderer,
                    Long.toString(r.missing()),
                    width / 2 + 150,
                    y,
                    r.missing() == 0
                            ? 0xFF55FF55
                            : 0xFFFF5555
            );

            y += rowHeight;
        }

        if (rows.isEmpty()) {
            c.drawCenteredTextWithShadow(
                    textRenderer,
                    "No materials found",
                    width / 2,
                    55,
                    0xFFFFFF55
            );
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (verticalAmount < 0) {
            scroll = Math.min(
                    Math.max(0, rows.size() - 1),
                    scroll + 1
            );
        } else if (verticalAmount > 0) {
            scroll = Math.max(0, scroll - 1);
        }

        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record Row(
            String name,
            long required,
            long have
    ) {
        long missing() {
            return Math.max(0, required - have);
        }
    }
}
