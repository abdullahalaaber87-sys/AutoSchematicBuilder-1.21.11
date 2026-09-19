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

    @Override protected void init() {
        try {
            Map<String, Long> needed = BuilderManager.getMaterials();
            List<Row> list = new ArrayList<>();
            for (var e : needed.entrySet()) {
                long have = countInventory(e.getKey());
                list.add(new Row(pretty(e.getKey()), e.getValue(), have));
            }
            list.sort(Comparator.comparingLong(Row::missing).reversed().thenComparing(Row::name));
            rows = list;
        } catch (Exception e) {
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> client.setScreen(parent))
                .dimensions(width / 2 - 50, height - 30, 100, 20).build());
    }

    private long countInventory(String blockId) {
        if (client == null || client.player == null) return 0;
        Identifier id = Identifier.tryParse(blockId);
        if (id == null) return 0;
        var block = Registries.BLOCK.get(id);
        var item = block.asItem();
        long count = 0;
        var inv = client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(item)) count += inv.getStack(i).getCount();
        return count;
    }

    private static String pretty(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder out = new StringBuilder();
        for (String p : s.split("_")) if (!p.isEmpty()) out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        return out.toString().trim();
    }

    @Override public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        c.drawCenteredTextWithShadow(textRenderer, "Materials - " + (BuilderManager.getSelectedName() == null ? "None" : BuilderManager.getSelectedName()), width/2, 16, 0xFFFFFF);
        if (error != null) {
            c.drawCenteredTextWithShadow(textRenderer, "Could not read schematic: " + error, width/2, 50, 0xFF5555);
            return;
        }
        c.drawTextWithShadow(textRenderer, "Material", width/2 - 190, 38, 0xAAAAAA);
        c.drawTextWithShadow(textRenderer, "Required", width/2 + 45, 38, 0xAAAAAA);
        c.drawTextWithShadow(textRenderer, "Have", width/2 + 105, 38, 0xAAAAAA);
        c.drawTextWithShadow(textRenderer, "Missing", width/2 + 150, 38, 0xAAAAAA);
        int visible = Math.max(1, (height - 95) / 12);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        int y = 54;
        for (int i = scroll; i < rows.size() && i < scroll + visible; i++, y += 12) {
            Row r = rows.get(i);
            c.drawTextWithShadow(textRenderer, r.name, width/2 - 190, y, 0xFFFFFF);
            c.drawTextWithShadow(textRenderer, Long.toString(r.required), width/2 + 45, y, 0xFFFFFF);
            c.drawTextWithShadow(textRenderer, Long.toString(r.have), width/2 + 105, y, r.have >= r.required ? 0x55FF55 : 0xFFFF55);
            c.drawTextWithShadow(textRenderer, Long.toString(r.missing()), width/2 + 150, y, r.missing() == 0 ? 0x55FF55 : 0xFF5555);
        }
        c.drawCenteredTextWithShadow(textRenderer, "Mouse wheel to scroll", width/2, height - 45, 0x777777);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) scroll--; else if (verticalAmount < 0) scroll++;
        return true;
    }

    @Override public boolean shouldPause() { return false; }
    private record Row(String name, long required, long have) { long missing() { return Math.max(0, required - have); } }
}
