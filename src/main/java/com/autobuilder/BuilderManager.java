package com.autobuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class BuilderManager {

    private static File selectedSchematic;
    private static String missingItem;
    
    private static int tickCounter = 0;
    private static int placementDelay = 3;
    private static final Random random = new Random();

    private BuilderManager() {
    }

    public static void select(File file) {
        selectedSchematic = file;
        missingItem = null;
        tickCounter = 0;

        AutoBuilderClient.message(
                MinecraftClient.getInstance(),
                "§aSelected: §f" + file.getName()
        );
    }

    public static boolean start(MinecraftClient client) {
        if (selectedSchematic == null) return false;
        AutoBuilderClient.state = AutoBuilderClient.BuildState.BUILDING;
        AutoBuilderClient.message(client, "§aStarted building schematic.");
        return true;
    }

    public static Map<String, Long> getMaterials() {
        Map<String, Long> materials = new HashMap<>();
        // Stub: Returns required materials map for MaterialsScreen
        return materials;
    }

    public static void tick(MinecraftClient client) {
        if (selectedSchematic == null ||
                client.player == null ||
                client.world == null ||
                client.interactionManager == null ||
                AutoBuilderClient.state != AutoBuilderClient.BuildState.BUILDING) {
            return;
        }

        if (tickCounter++ < placementDelay) {
            return;
        }
        
        placementDelay = 2 + random.nextInt(3); 
        tickCounter = 0;

        processBypassPlacement(client);
    }

    private static void processBypassPlacement(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        World world = client.world;

        BlockPos targetPos = findNextMissingBlock(world);
        if (targetPos == null) {
            AutoBuilderClient.message(client, "§aBuild complete!");
            stop();
            return;
        }

        if (player.getBlockPos().getSquaredDistance(targetPos) > 16.0) {
            return;
        }

        int slot = findRequiredItemSlot(player);
        if (slot == -1) {
            setMissingItem("Required Block");
            return;
        }

        if (slot < 9) {
            player.getInventory().selectedSlot = slot;
        }

        performBypassPlacement(client, targetPos);
    }

    private static void performBypassPlacement(MinecraftClient client, BlockPos pos) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.getNetworkHandler() == null) return;

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Direction side = Direction.UP;

        double diffX = hitVec.x - player.getX();
        double diffY = hitVec.y - (player.getY() + player.getEyeHeight(player.getPose()));
        double diffZ = hitVec.z - player.getZ();
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.atan2(diffZ, diffX) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * (180.0 / Math.PI)));

        client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw, pitch, player.isOnGround(), player.horizontalCollision
        ));

        BlockHitResult hitResult = new BlockHitResult(hitVec, side, pos, false);
        
        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(
                Hand.MAIN_HAND,
                hitResult,
                0
        );

        client.getNetworkHandler().sendPacket(packet);
        player.swingHand(Hand.MAIN_HAND);
    }

    private static BlockPos findNextMissingBlock(World world) {
        return null;
    }

    private static int findRequiredItemSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hasBuild() {
        return selectedSchematic != null;
    }

    public static String getSelectedName() {
        return selectedSchematic == null ? null : selectedSchematic.getName();
    }

    public static String getMissingItem() {
        return missingItem;
    }

    public static void setMissingItem(String itemName) {
        missingItem = itemName;
        AutoBuilderClient.state = AutoBuilderClient.BuildState.PAUSED;
        AutoBuilderClient.message(MinecraftClient.getInstance(), "§cMissing material: §f" + itemName);
    }

    public static void stop() {
        selectedSchematic = null;
        missingItem = null;
        tickCounter = 0;
        AutoBuilderClient.state = AutoBuilderClient.BuildState.IDLE;
    }
}
