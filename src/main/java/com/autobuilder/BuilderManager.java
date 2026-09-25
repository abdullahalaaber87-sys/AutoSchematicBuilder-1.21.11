package com.autobuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class BuilderManager {
    private static File selectedSchematic;
    private static String missingItem;
    private static List<LitematicBlueprint.BlockEntry> blocks = List.of();
    private static BlockPos origin;
    private static int tickDelay;
    private static int stalledTicks;

    private BuilderManager() {}

    public static void select(File file) {
        selectedSchematic = file;
        missingItem = null;
        blocks = List.of();
        origin = null;
        AutoBuilderClient.message(MinecraftClient.getInstance(), "§aSelected: §f" + file.getName());
    }

    public static boolean start(MinecraftClient client) {
        if (selectedSchematic == null || client.player == null) return false;
        try {
            blocks = LitematicBlueprint.read(selectedSchematic);
            origin = client.player.getBlockPos();
            missingItem = null;
            tickDelay = 0;
            stalledTicks = 0;
            AutoBuilderClient.message(client, "§aBuild origin: §f" + origin.getX() + " " + origin.getY() + " " + origin.getZ());
            AutoBuilderClient.message(client, "§7Blocks loaded: §f" + blocks.size());
            return !blocks.isEmpty();
        } catch (Exception e) {
            AutoBuilderClient.message(client, "§cCould not read schematic: §f" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static void tick(MinecraftClient client) {
        if (selectedSchematic == null || client.player == null || client.world == null || client.interactionManager == null) return;
        if (origin == null && !start(client)) {
            AutoBuilderClient.state = AutoBuilderClient.BuildState.STOPPED;
            return;
        }
        if (tickDelay-- > 0) return;
        tickDelay = 2;

        int unfinished = 0;
        boolean reachableFound = false;

        for (LitematicBlueprint.BlockEntry entry : blocks) {
            BlockPos target = origin.add(entry.x(), entry.y(), entry.z());
            Identifier id = Identifier.tryParse(entry.blockId());
            if (id == null) continue;
            var wantedBlock = Registries.BLOCK.get(id);
            var current = client.world.getBlockState(target);
            if (current.isOf(wantedBlock)) continue;
            unfinished++;

            if (!current.isReplaceable()) continue;
            if (client.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(target)) > 20.25) continue;

            PlacementSide side = findSupport(client, target);
            if (side == null) continue;
            reachableFound = true;

            int slot = findHotbarSlot(client, id);
            if (slot < 0) {
                setMissingItem(pretty(entry.blockId()) + " (put it in hotbar)");
                return;
            }

            client.player.getInventory().setSelectedSlot(slot);
            Vec3d hitPos = Vec3d.ofCenter(side.support()).add(
                    side.face().getOffsetX() * 0.5,
                    side.face().getOffsetY() * 0.5,
                    side.face().getOffsetZ() * 0.5
            );
            BlockHitResult hit = new BlockHitResult(hitPos, side.face(), side.support(), false);
            var result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
            if (!result.isAccepted()) {
                stalledTicks++;
                if (stalledTicks == 40) AutoBuilderClient.message(client, "§ePlacement was rejected; check reach, support and server rules.");
                return;
            }
            client.player.swingHand(Hand.MAIN_HAND);
            stalledTicks = 0;
            return;
        }

        if (unfinished == 0) {
            AutoBuilderClient.state = AutoBuilderClient.BuildState.STOPPED;
            AutoBuilderClient.message(client, "§aBuild complete!");
            return;
        }

        if (!reachableFound) {
            stalledTicks++;
            if (stalledTicks == 40) {
                AutoBuilderClient.message(client, "§eMove closer to the next part of the schematic. Builder is waiting.");
            }
        }
    }

    private static PlacementSide findSupport(MinecraftClient client, BlockPos target) {
        Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
        for (Direction d : order) {
            BlockPos support = target.offset(d);
            if (!client.world.getBlockState(support).isReplaceable()) {
                return new PlacementSide(support, d.getOpposite());
            }
        }
        return null;
    }

    private static int findHotbarSlot(MinecraftClient client, Identifier id) {
        Item item = Registries.BLOCK.get(id).asItem();
        for (int i = 0; i < 9; i++) {
            var stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) return i;
        }
        return -1;
    }

    private static String pretty(String id) {
        int colon = id.indexOf(':');
        String s = colon >= 0 ? id.substring(colon + 1) : id;
        return s.replace('_', ' ');
    }

    private record PlacementSide(BlockPos support, Direction face) {}

    public static boolean hasBuild() { return selectedSchematic != null; }
    public static String getSelectedName() { return selectedSchematic == null ? null : selectedSchematic.getName(); }
    public static Map<String, Long> getMaterials() throws IOException { return selectedSchematic == null ? Map.of() : LitematicMaterials.read(selectedSchematic); }
    public static String getMissingItem() { return missingItem; }

    public static void setMissingItem(String itemName) {
        if (itemName.equals(missingItem)) return;
        missingItem = itemName;
        AutoBuilderClient.state = AutoBuilderClient.BuildState.PAUSED;
        AutoBuilderClient.message(MinecraftClient.getInstance(), "§cMissing material: §f" + itemName);
    }

    public static void stop() {
        missingItem = null;
        blocks = List.of();
        origin = null;
        tickDelay = 0;
        stalledTicks = 0;
    }
}
