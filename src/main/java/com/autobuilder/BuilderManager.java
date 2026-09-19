package com.autobuilder;

import net.minecraft.client.MinecraftClient;

import java.io.File;

public final class BuilderManager {

    private static File selectedSchematic;
    private static String missingItem;

    private BuilderManager() {
    }

    public static void select(File file) {
        selectedSchematic = file;
        missingItem = null;

        AutoBuilderClient.message(
                MinecraftClient.getInstance(),
                "§aSelected: §f" + file.getName()
        );
    }

    public static void tick(MinecraftClient client) {
        if (selectedSchematic == null ||
                client.player == null ||
                client.world == null ||
                client.interactionManager == null) {
            return;
        }

        /*
         * Auto building code will run here.
         *
         * It will:
         * - Read the selected Litematica schematic.
         * - Find the next block that needs placing.
         * - Check the player's inventory.
         * - Select the correct material.
         * - Place the block using normal client interaction.
         * - If the material runs out, pause and show the missing item.
         */
    }

    public static boolean hasBuild() {
        return selectedSchematic != null;
    }

    public static String getSelectedName() {
        return selectedSchematic == null
                ? null
                : selectedSchematic.getName();
    }

    public static String getMissingItem() {
        return missingItem;
    }

    public static void setMissingItem(String itemName) {
        missingItem = itemName;

        AutoBuilderClient.state =
                AutoBuilderClient.BuildState.PAUSED;

        AutoBuilderClient.message(
                MinecraftClient.getInstance(),
                "§cMissing material: §f" + itemName
        );
    }

    public static void stop() {
        selectedSchematic = null;
        missingItem = null;
    }
}
