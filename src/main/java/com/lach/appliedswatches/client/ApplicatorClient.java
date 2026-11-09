package com.lach.appliedswatches.client;

import com.lach.appliedswatches.AppliedSwatches;
import com.lach.appliedswatches.ApplicatorData;
import com.lach.appliedswatches.ApplicatorItem;
import com.lach.appliedswatches.PlaceMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
import com.lach.appliedswatches.net.CycleApplicatorPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Applicator rendering and sneak + scroll selection. */
@EventBusSubscriber(modid = AppliedSwatches.MODID, value = Dist.CLIENT)
public final class ApplicatorClient {
    private ApplicatorClient() {
    }

    @SubscribeEvent
    static void recipes(net.neoforged.neoforge.client.event.RecipesReceivedEvent event) {
        TerminalScreen.setRecipes(event.getRecipeMap());
    }

    @SubscribeEvent
    static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        appeng.api.client.StorageCellModels.registerModel(AppliedSwatches.CREATIVE_CELL.get(),
                net.minecraft.resources.Identifier.fromNamespaceAndPath("ae2", "block/drive_creative_cell"));
    }

    @SubscribeEvent
    static void specialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(AppliedSwatches.id("applicator"), ApplicatorRenderer.Unbaked.MAP_CODEC);
    }

    /** Outlines where the next click will place blocks. */
    @SubscribeEvent
    static void preview(ExtractBlockOutlineRenderStateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof ApplicatorItem)) held = mc.player.getOffhandItem();
        if (!(held.getItem() instanceof ApplicatorItem) || mc.player.isShiftKeyDown()) return;
        ApplicatorData d = ApplicatorItem.data(held);
        if (d.blocks().isEmpty() || d.mode() == PlaceMode.SINGLE) return;
        BlockHitResult hit = event.getHitResult();
        List<BlockPos> targets = d.mode().targets(event.getLevel(), hit.getBlockPos(), hit.getDirection(), d.size());
        if (targets.isEmpty()) return;
        BlockPos origin = event.getBlockPos();
        VoxelShape shape = Shapes.empty();
        for (BlockPos p : targets)
            shape = Shapes.joinUnoptimized(shape, Shapes.create(new AABB(p.subtract(origin)).deflate(0.04)), net.minecraft.world.phys.shapes.BooleanOp.OR);
        VoxelShape outline = shape;
        event.addCustomRenderer((state, out, pose, level) -> {
            var cam = level.cameraRenderState.pos;
            pose.pushPose();
            pose.translate(origin.getX() - cam.x, origin.getY() - cam.y, origin.getZ() - cam.z);
            out.submitShapeOutline(pose, outline, RenderTypes.lines(), 0xE6B880FF, 2f, state.isTranslucent());
            pose.popPose();
            return false;
        });
    }

    @SubscribeEvent
    static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null || !mc.player.isShiftKeyDown() || event.getScrollDeltaY() == 0) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof ApplicatorItem)) return;
        ClientPacketDistributor.sendToServer(new CycleApplicatorPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
        event.setCanceled(true);
    }
}
