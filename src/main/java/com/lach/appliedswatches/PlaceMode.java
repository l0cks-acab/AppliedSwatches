package com.lach.appliedswatches;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** How many blocks one applicator click places, and where. Shared by placement and the client preview. */
public enum PlaceMode implements StringRepresentable {
    /** One block against the clicked face. */
    SINGLE(1, 1, 1),
    /** Building-wand style: copies the clicked face outward across the connected surface of the same block. */
    WAND(3, 9, 2),
    /** A line growing straight out of the clicked face. */
    LINE(2, 16, 1),
    /** A square centred on the clicked face, flat against it. */
    SQUARE(3, 9, 2);

    public static final com.mojang.serialization.Codec<PlaceMode> CODEC = StringRepresentable.fromEnum(PlaceMode::values);
    public static final StreamCodec<ByteBuf, PlaceMode> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(i -> values()[Math.floorMod(i, values().length)], Enum::ordinal);

    public final int minSize, maxSize, step;

    PlaceMode(int minSize, int maxSize, int step) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.step = step;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String key() {
        return "applicator_mode.appliedswatches." + getSerializedName();
    }

    public int clampSize(int size) {
        int s = Math.max(minSize, Math.min(maxSize, size));
        return minSize + (s - minSize) / step * step;
    }

    /** Positions to fill, nearest the click first, all currently replaceable. */
    public List<BlockPos> targets(Level level, BlockPos clicked, Direction face, int size) {
        size = clampSize(size);
        BlockPos start = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(face);
        List<BlockPos> out = new ArrayList<>();
        switch (this) {
            case SINGLE -> out.add(start);
            case LINE -> {
                for (int i = 0; i < size; i++) {
                    BlockPos p = start.relative(face, i);
                    if (!free(level, p)) break;
                    out.add(p);
                }
            }
            case SQUARE -> {
                int r = size / 2;
                Direction[] axes = planeAxes(face);
                for (int a = -r; a <= r; a++)
                    for (int b = -r; b <= r; b++) out.add(start.relative(axes[0], a).relative(axes[1], b));
                out.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(start)));
            }
            case WAND -> out.addAll(wand(level, clicked, face, size / 2));
        }
        out.removeIf(p -> !free(level, p));
        return out;
    }

    /** Flood-fills the clicked block's face plane through blocks of the same kind whose outer side is open. */
    private static List<BlockPos> wand(Level level, BlockPos clicked, Direction face, int r) {
        BlockState state = level.getBlockState(clicked);
        if (state.canBeReplaced()) return List.of(clicked);
        Block kind = state.getBlock();
        Direction[] axes = planeAxes(face);
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(clicked);
        seen.add(clicked);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (!level.getBlockState(p).is(kind) || !free(level, p.relative(face))) continue;
            out.add(p.relative(face));
            for (Direction d : new Direction[]{axes[0], axes[0].getOpposite(), axes[1], axes[1].getOpposite()}) {
                BlockPos n = p.relative(d);
                BlockPos off = n.subtract(clicked);
                if (Math.abs(off.getX()) > r || Math.abs(off.getY()) > r || Math.abs(off.getZ()) > r) continue;
                if (seen.add(n)) queue.add(n);
            }
        }
        return out;
    }

    private static boolean free(Level level, BlockPos p) {
        return level.isInWorldBounds(p) && level.getBlockState(p).canBeReplaced();
    }

    private static Direction[] planeAxes(Direction face) {
        return switch (face.getAxis()) {
            case X -> new Direction[]{Direction.UP, Direction.SOUTH};
            case Y -> new Direction[]{Direction.EAST, Direction.SOUTH};
            case Z -> new Direction[]{Direction.UP, Direction.EAST};
        };
    }
}
