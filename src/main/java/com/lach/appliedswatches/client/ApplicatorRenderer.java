package com.lach.appliedswatches.client;

import com.lach.appliedswatches.AppliedSwatches;
import com.lach.appliedswatches.ApplicatorData;
import com.lach.appliedswatches.ApplicatorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.List;

/** Draws the applicator's model, then the selected block's texture on its screen. */
public class ApplicatorRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ModelResourceLocation BASE = ModelResourceLocation.standalone(AppliedSwatches.id("item/swatch_applicator_base"));
    /** Screen area on the model's top face, in model pixels. */
    private static final float X0 = 4.5f, X1 = 8.5f, Z0 = 6f, Z1 = 10f, Y = 13.01f;

    public ApplicatorRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(BASE);
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, Sheets.cutoutBlockSheet(), true, stack.hasFoil());
        mc.getItemRenderer().renderModelLists(model, stack, light, overlay, pose, vc);

        Item shown = shownBlock(ApplicatorItem.data(stack));
        if (!(shown instanceof BlockItem block)) return;
        BlockState state = block.getBlock().defaultBlockState();
        TextureAtlasSprite sprite = faceSprite(mc, state);
        int tint = mc.getBlockColors().getColor(state, null, null, 0);
        int rgb = tint == -1 ? 0xFFFFFF : tint;
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        Matrix4f m = pose.last().pose();
        VertexConsumer screen = buffers.getBuffer(Sheets.cutoutBlockSheet());
        float x0 = X0 / 16, x1 = X1 / 16, z0 = Z0 / 16, z1 = Z1 / 16, y = Y / 16;
        int full = LightTexture.FULL_BRIGHT;
        screen.addVertex(m, x0, y, z0).setColor(r, g, b, 1).setUv(sprite.getU0(), sprite.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(pose.last(), 0, 1, 0);
        screen.addVertex(m, x0, y, z1).setColor(r, g, b, 1).setUv(sprite.getU1(), sprite.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(pose.last(), 0, 1, 0);
        screen.addVertex(m, x1, y, z1).setColor(r, g, b, 1).setUv(sprite.getU1(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(pose.last(), 0, 1, 0);
        screen.addVertex(m, x1, y, z0).setColor(r, g, b, 1).setUv(sprite.getU0(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(pose.last(), 0, 1, 0);

        if (ctx != ItemDisplayContext.GUI) renderHologram(mc, state, pose, buffers, r, g, b);
    }

    /** A small see-through copy of the block spinning above the screen, with a faint projection cone. */
    private static void renderHologram(Minecraft mc, BlockState state, PoseStack pose, MultiBufferSource buffers, float r, float g, float b) {
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        float t = (System.currentTimeMillis() % 6000L) / 6000f;
        float bob = (float) Math.sin(t * Math.PI * 4) * 0.012f;
        float cx = (X0 + X1) / 32, cz = (Z0 + Z1) / 32, base = Y / 16 + 0.09f + bob, size = 0.16f;

        // Projection cone: from the screen's corners up to the hologram's base.
        Matrix4f m = pose.last().pose();
        VertexConsumer beam = buffers.getBuffer(RenderType.lightning());
        float[][] lo = {{X0 / 16, Z0 / 16}, {X0 / 16, Z1 / 16}, {X1 / 16, Z1 / 16}, {X1 / 16, Z0 / 16}};
        float h = size * 0.45f;
        float[][] hi = {{cx - h, cz - h}, {cx - h, cz + h}, {cx + h, cz + h}, {cx + h, cz - h}};
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            beam.addVertex(m, lo[i][0], Y / 16, lo[i][1]).setColor(0.35f, 0.85f, 1f, 0.10f);
            beam.addVertex(m, lo[j][0], Y / 16, lo[j][1]).setColor(0.35f, 0.85f, 1f, 0.10f);
            beam.addVertex(m, hi[j][0], base, hi[j][1]).setColor(0.35f, 0.85f, 1f, 0.02f);
            beam.addVertex(m, hi[i][0], base, hi[i][1]).setColor(0.35f, 0.85f, 1f, 0.02f);
        }

        pose.pushPose();
        pose.translate(cx, base, cz);
        pose.mulPose(Axis.YP.rotationDegrees(t * 360));
        pose.scale(size, size, size);
        pose.translate(-0.5f, 0, -0.5f);
        // Flicker a little, like a projection.
        float alpha = 0.55f + 0.08f * (float) Math.sin(t * Math.PI * 46);
        VertexConsumer holo = new Tinted(buffers.getBuffer(Sheets.translucentItemSheet()), alpha);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), holo, state, model, r * 0.7f + 0.15f, g * 0.85f + 0.15f, b * 0.8f + 0.2f, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        pose.popPose();
    }

    /** Passes vertices through with their alpha scaled, for the see-through hologram. */
    private record Tinted(VertexConsumer inner, float alpha) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            inner.setColor(r, g, b, (int) (a * alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            inner.setNormal(x, y, z);
            return this;
        }
    }

    /** The selected block, or in random mode a different palette block every half second. */
    private static Item shownBlock(ApplicatorData d) {
        if (d.blocks().isEmpty()) return null;
        if (d.selected() != ApplicatorData.RANDOM) return d.selectedBlock().orElse(null);
        long t = System.currentTimeMillis() / 500;
        return d.blocks().get((int) (t % d.blocks().size()));
    }

    /** Sprite of the block's north face, falling back to its particle texture. */
    private static TextureAtlasSprite faceSprite(Minecraft mc, BlockState state) {
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        List<net.minecraft.client.renderer.block.model.BakedQuad> quads = model.getQuads(state, Direction.NORTH, net.minecraft.util.RandomSource.create(42), ModelData.EMPTY, null);
        return quads.isEmpty() ? model.getParticleIcon(ModelData.EMPTY) : quads.get(0).getSprite();
    }
}
