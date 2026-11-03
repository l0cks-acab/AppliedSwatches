package com.lach.appliedswatches.client;

import com.lach.appliedswatches.ApplicatorData;
import com.lach.appliedswatches.ApplicatorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** Draws the selected block's texture on the applicator's screen, and outside the GUI a hologram of it above. */
public record ApplicatorRenderer(boolean hologram) implements SpecialModelRenderer<BlockState> {
    /** Screen area on the head's top face, in model pixels, before the head's tilt. */
    private static final float X0 = 5.5f, X1 = 10f, Z0 = 5.75f, Z1 = 10.25f, Y = 15.26f;
    /** Head tilt about z, matching tools/applicator_model.py (HEAD_ANGLE, HEAD_ORIGIN). */
    private static final float HEAD_ANGLE = 22.5f, HEAD_OX = 8f, HEAD_OY = 13.5f;
    private static final int FULL = LightCoordsUtil.FULL_BRIGHT;

    @Override
    public @Nullable BlockState extractArgument(ItemStack stack) {
        Item shown = shownBlock(ApplicatorItem.data(stack));
        return shown instanceof BlockItem block ? block.getBlock().defaultBlockState() : null;
    }

    @Override
    public void submit(@Nullable BlockState state, PoseStack pose, SubmitNodeCollector out, int light, int overlay, boolean foil, int outline) {
        if (state == null) return;
        TextureAtlasSprite sprite = faceSprite(state);
        int tint = ModelQuads.tint(state, 0);
        int rgb = tint == -1 ? 0xFFFFFF : tint;
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        out.submitCustomGeometry(pose, Sheets.cutoutBlockSheet(), (p, vc) -> {
            Matrix4f m = tilted(p.pose());
            float x0 = X0 / 16, x1 = X1 / 16, z0 = Z0 / 16, z1 = Z1 / 16, y = Y / 16;
            vc.addVertex(m, x0, y, z0).setColor(r, g, b, 1).setUv(sprite.getU0(), sprite.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0, 1, 0);
            vc.addVertex(m, x0, y, z1).setColor(r, g, b, 1).setUv(sprite.getU1(), sprite.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0, 1, 0);
            vc.addVertex(m, x1, y, z1).setColor(r, g, b, 1).setUv(sprite.getU1(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0, 1, 0);
            vc.addVertex(m, x1, y, z0).setColor(r, g, b, 1).setUv(sprite.getU0(), sprite.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0, 1, 0);
        });
        if (hologram) submitHologram(state, pose, out, r, g, b);
    }

    /** A small see-through copy of the block spinning above the screen, with a pale fluix-purple projection cone. */
    private static void submitHologram(BlockState state, PoseStack pose, SubmitNodeCollector out, float r, float g, float b) {
        float t = (System.currentTimeMillis() % 6000L) / 6000f;
        float bob = (float) Math.sin(t * Math.PI * 4) * 0.012f;
        // Screen centre after the tilt; the hologram floats straight up from it.
        double rad = Math.toRadians(HEAD_ANGLE), dx = (X0 + X1) / 2 - HEAD_OX, dy = Y - HEAD_OY;
        float cx = (float) (HEAD_OX + dx * Math.cos(rad) - dy * Math.sin(rad)) / 16, cz = (Z0 + Z1) / 32;
        float base = (float) (HEAD_OY + dx * Math.sin(rad) + dy * Math.cos(rad)) / 16 + 0.12f + bob, size = 0.16f;

        // Projection cone: from the screen's corners up to the hologram's base.
        float h = size * 0.45f;
        out.submitCustomGeometry(pose, RenderTypes.lightning(), (p, beam) -> {
            Matrix4f m = p.pose(), mt = tilted(m);
            float[][] lo = {{X0 / 16, Z0 / 16}, {X0 / 16, Z1 / 16}, {X1 / 16, Z1 / 16}, {X1 / 16, Z0 / 16}};
            float[][] hi = {{cx - h, cz - h}, {cx - h, cz + h}, {cx + h, cz + h}, {cx + h, cz - h}};
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                beam.addVertex(mt, lo[i][0], Y / 16, lo[i][1]).setColor(0.62f, 0.32f, 1f, 0.55f);
                beam.addVertex(mt, lo[j][0], Y / 16, lo[j][1]).setColor(0.62f, 0.32f, 1f, 0.55f);
                beam.addVertex(m, hi[j][0], base, hi[j][1]).setColor(0.62f, 0.32f, 1f, 0.10f);
                beam.addVertex(m, hi[i][0], base, hi[i][1]).setColor(0.62f, 0.32f, 1f, 0.10f);
            }
        });

        pose.pushPose();
        pose.translate(cx, base, cz);
        pose.mulPose(Axis.YP.rotationDegrees(t * 360));
        pose.scale(size, size, size);
        pose.translate(-0.5f, 0, -0.5f);
        // Flicker a little, like a projection.
        float alpha = 0.55f + 0.08f * (float) Math.sin(t * Math.PI * 46);
        List<BakedQuad> quads = ModelQuads.quads(state, null);
        float hr = r * 0.7f + 0.15f, hg = g * 0.85f + 0.15f, hb = b * 0.8f + 0.2f;
        out.submitCustomGeometry(pose, Sheets.translucentBlockItemSheet(), (p, vc) -> {
            QuadInstance inst = new QuadInstance();
            inst.setLightCoords(FULL);
            inst.setOverlayCoords(OverlayTexture.NO_OVERLAY);
            for (BakedQuad q : quads) {
                float shade = q.materialInfo().shade() ? shade(q.direction()) : 1f;
                inst.setColor(argb(alpha, hr * shade, hg * shade, hb * shade));
                vc.putBakedQuad(p, q, inst);
            }
        });
        pose.popPose();
    }

    /** The pose with the head's tilt applied, for points on the screen. */
    private static Matrix4f tilted(Matrix4f m) {
        return new Matrix4f(m).rotateAround(Axis.ZP.rotationDegrees(HEAD_ANGLE), HEAD_OX / 16, HEAD_OY / 16, 0);
    }

    private static float shade(Direction d) {
        return switch (d) {
            case UP -> 1f;
            case DOWN -> 0.5f;
            case NORTH, SOUTH -> 0.8f;
            default -> 0.6f;
        };
    }

    private static int argb(float a, float r, float g, float b) {
        return (int) (a * 255) << 24 | (int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0, 0, 0));
        output.accept(new Vector3f(1, 1, 1));
    }

    /** The selected block, or in random mode a different palette block every half second. */
    private static @Nullable Item shownBlock(ApplicatorData d) {
        if (d.blocks().isEmpty()) return null;
        if (d.selected() != ApplicatorData.RANDOM) return d.selectedBlock().orElse(null);
        long t = System.currentTimeMillis() / 500;
        return d.blocks().get((int) (t % d.blocks().size()));
    }

    /** Sprite of the block's north face, falling back to its particle texture. */
    private static TextureAtlasSprite faceSprite(BlockState state) {
        List<BakedQuad> quads = ModelQuads.quads(state, Direction.NORTH);
        return quads.isEmpty() ? ModelQuads.particle(state) : quads.get(0).materialInfo().sprite();
    }

    public record Unbaked(boolean hologram) implements SpecialModelRenderer.Unbaked<BlockState> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.BOOL.optionalFieldOf("hologram", true).forGetter(Unbaked::hologram)).apply(i, Unbaked::new));

        @Override
        public SpecialModelRenderer<BlockState> bake(BakingContext context) {
            return new ApplicatorRenderer(hologram);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
