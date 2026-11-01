package com.lach.appliedswatches;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

/**
 * What a Swatch Applicator holds: the palette's blocks, which one is selected ({@link #RANDOM} picks any of them
 * per placement), the palette's name and the terminal it draws from.
 */
public record ApplicatorData(List<Item> blocks, int selected, String name, Optional<GlobalPos> link, PlaceMode mode, int size) {
    public static final int RANDOM = -1;
    public static final ApplicatorData EMPTY = new ApplicatorData(List.of(), RANDOM, "", Optional.empty(), PlaceMode.SINGLE, 3);

    public ApplicatorData {
        size = mode.clampSize(size);
    }

    public static final Codec<ApplicatorData> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("blocks").forGetter(ApplicatorData::blocks),
            Codec.INT.fieldOf("selected").forGetter(ApplicatorData::selected),
            Codec.STRING.fieldOf("name").forGetter(ApplicatorData::name),
            GlobalPos.CODEC.optionalFieldOf("link").forGetter(ApplicatorData::link),
            PlaceMode.CODEC.optionalFieldOf("mode", PlaceMode.SINGLE).forGetter(ApplicatorData::mode),
            Codec.INT.optionalFieldOf("size", 3).forGetter(ApplicatorData::size)
    ).apply(i, ApplicatorData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplicatorData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(SwatchTerminalBlockEntity.SLOTS)), ApplicatorData::blocks,
            ByteBufCodecs.VAR_INT, ApplicatorData::selected,
            ByteBufCodecs.STRING_UTF8, ApplicatorData::name,
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), ApplicatorData::link,
            PlaceMode.STREAM_CODEC, ApplicatorData::mode,
            ByteBufCodecs.VAR_INT, ApplicatorData::size,
            ApplicatorData::new);

    /** Steps the selection by {@code dir}, through each block and then random. */
    public ApplicatorData cycle(int dir) {
        int n = blocks.size() + 1, cur = selected == RANDOM ? blocks.size() : selected;
        int next = Math.floorMod(cur + dir, n);
        return withSelected(next == blocks.size() ? RANDOM : next);
    }

    public ApplicatorData withSelected(int selected) {
        return new ApplicatorData(blocks, selected >= blocks.size() ? RANDOM : selected, name, link, mode, size);
    }

    public ApplicatorData withPlacement(PlaceMode mode, int size) {
        return new ApplicatorData(blocks, selected, name, link, mode, size);
    }

    public Optional<Item> selectedBlock() {
        return selected >= 0 && selected < blocks.size() ? Optional.of(blocks.get(selected)) : Optional.empty();
    }
}
