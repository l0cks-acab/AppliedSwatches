package com.lach.appliedswatches.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Average on-screen colour of each block, worked out from its model's textures. Cleared on resource reload. */
public final class BlockColorCache {
    /** rgb 0xRRGGBB, lab, whether the block is a full cube. */
    public record Info(int rgb, float[] lab, boolean full) {
    }

    private static final Map<Item, Info> BLOCKS = new HashMap<>();
    private static final Map<ResourceLocation, int[]> SPRITES = new HashMap<>();
    private static final Direction[] SIDES = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private BlockColorCache() {
    }

    public static void clear() {
        BLOCKS.clear();
        SPRITES.clear();
    }

    public static @Nullable Info get(Item item) {
        if (!(item instanceof BlockItem bi)) return null;
        return BLOCKS.computeIfAbsent(item, i -> compute(bi.getBlock().defaultBlockState()));
    }

    private static @Nullable Info compute(BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getBlockRenderer().getBlockModel(state);
        RandomSource rand = RandomSource.create();
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction d : SIDES) {
            rand.setSeed(42);
            try {
                quads.addAll(model.getQuads(state, d, rand, ModelData.EMPTY, null));
            } catch (RuntimeException ignored) {
            }
        }
        long r = 0, g = 0, b = 0, n = 0;
        if (quads.isEmpty()) {
            int[] c = sprite(model.getParticleIcon(ModelData.EMPTY));
            if (c[3] == 0) return null;
            r = c[0];
            g = c[1];
            b = c[2];
            n = 1;
        }
        for (BakedQuad q : quads) {
            int[] c = sprite(q.getSprite());
            if (c[3] == 0) continue;
            int cr = c[0], cg = c[1], cb = c[2];
            if (q.isTinted()) {
                int tint = mc.getBlockColors().getColor(state, null, null, q.getTintIndex());
                if (tint != -1) {
                    cr = cr * ((tint >> 16) & 0xFF) / 255;
                    cg = cg * ((tint >> 8) & 0xFF) / 255;
                    cb = cb * (tint & 0xFF) / 255;
                }
            }
            r += (long) cr * c[3];
            g += (long) cg * c[3];
            b += (long) cb * c[3];
            n += c[3];
        }
        if (n == 0) return null;
        int rgb = (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
        boolean full;
        try {
            full = state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && state.canOcclude();
        } catch (RuntimeException e) {
            full = false;
        }
        return new Info(rgb, ColorMath.lab(rgb), full);
    }

    /** Mean r, g, b of the sprite's opaque pixels plus how many there were (as the 4th value). */
    private static int[] sprite(TextureAtlasSprite sprite) {
        return SPRITES.computeIfAbsent(sprite.contents().name(), k -> {
            SpriteContents c = sprite.contents();
            var img = c.getOriginalImage();
            int w = Math.min(c.width(), img.getWidth()), h = Math.min(c.height(), img.getHeight());
            long r = 0, g = 0, b = 0;
            int n = 0;
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int p = img.getPixelRGBA(x, y);
                    if ((p >>> 24) < 128) continue;
                    r += p & 0xFF;
                    g += (p >> 8) & 0xFF;
                    b += (p >> 16) & 0xFF;
                    n++;
                }
            return n == 0 ? new int[4] : new int[]{(int) (r / n), (int) (g / n), (int) (b / n), n};
        });
    }
}
