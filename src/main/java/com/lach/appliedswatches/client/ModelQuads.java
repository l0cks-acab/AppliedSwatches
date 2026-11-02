package com.lach.appliedswatches.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Block model lookups shared by the colour cache and the applicator renderer. */
public final class ModelQuads {
    private static final Direction[] SIDES = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private ModelQuads() {
    }

    public static BlockStateModel model(BlockState state) {
        return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
    }

    public static List<BlockStateModelPart> parts(BlockState state) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model(state).collectParts(RandomSource.create(42), parts);
        return parts;
    }

    /** All quads of the block's model, culled faces included. Faces of {@code only} when given. */
    public static List<BakedQuad> quads(BlockState state, Direction only) {
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts(state)) {
            if (only != null) {
                quads.addAll(part.getQuads(only));
                continue;
            }
            for (Direction d : SIDES) {
                try {
                    quads.addAll(part.getQuads(d));
                } catch (RuntimeException ignored) {
                }
            }
        }
        return quads;
    }

    public static TextureAtlasSprite particle(BlockState state) {
        return model(state).particleMaterial().sprite();
    }

    /** Tint colour (0xRRGGBB) of the given layer, or -1 when untinted. */
    public static int tint(BlockState state, int layer) {
        BlockTintSource src = Minecraft.getInstance().getBlockColors().getTintSource(state, layer);
        return src == null ? -1 : src.color(state);
    }
}
