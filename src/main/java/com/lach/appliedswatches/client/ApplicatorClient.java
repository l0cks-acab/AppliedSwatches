package com.lach.appliedswatches.client;

import com.lach.appliedswatches.AppliedSwatches;
import com.lach.appliedswatches.ApplicatorData;
import com.lach.appliedswatches.ApplicatorItem;
import com.lach.appliedswatches.PlaceMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
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
    static void specialRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(AppliedSwatches.id("applicator"), ApplicatorRenderer.Unbaked.MAP_CODEC);
    }

    /** Outlines where the next click will place blocks. */
    @SubscribeEvent
    static void preview(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof ApplicatorItem)) held = mc.player.getOffhandItem();
        if (!(held.getItem() instanceof ApplicatorItem) || mc.player.isShiftKeyDown()) return;
        ApplicatorData d = ApplicatorItem.data(held);
        if (d.blocks().isEmpty() || d.mode() == PlaceMode.SINGLE) return;
        List<BlockPos> targets = d.mode().targets(mc.level, hit.getBlockPos(), hit.getDirection(), d.size());
        if (targets.isEmpty()) return;
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack pose = event.getPoseStack();
        if (pose == null) return;
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
        for (BlockPos p : targets)
            ShapeRenderer.renderShape(pose, lines, Shapes.create(new AABB(p).deflate(0.04)), -cam.x, -cam.y, -cam.z, 0xE6B880FF, 2f);
        buffers.endBatch(RenderTypes.lines());
    }

    @SubscribeEvent
    static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown() || event.getScrollDeltaY() == 0) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof ApplicatorItem)) return;
        ClientPacketDistributor.sendToServer(new CycleApplicatorPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
        event.setCanceled(true);
    }
}
