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
public record ApplicatorData(List<Item> blocks, int selected, String name, Optional<GlobalPos> link) {
    public static final int RANDOM = -1;
    public static final ApplicatorData EMPTY = new ApplicatorData(List.of(), RANDOM, "", Optional.empty());

    public static final Codec<ApplicatorData> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("blocks").forGetter(ApplicatorData::blocks),
            Codec.INT.fieldOf("selected").forGetter(ApplicatorData::selected),
            Codec.STRING.fieldOf("name").forGetter(ApplicatorData::name),
            GlobalPos.CODEC.optionalFieldOf("link").forGetter(ApplicatorData::link)
    ).apply(i, ApplicatorData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ApplicatorData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list(SwatchTerminalBlockEntity.SLOTS)), ApplicatorData::blocks,
            ByteBufCodecs.VAR_INT, ApplicatorData::selected,
            ByteBufCodecs.STRING_UTF8, ApplicatorData::name,
            GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), ApplicatorData::link,
            ApplicatorData::new);

    /** Steps the selection by {@code dir}, through each block and then random. */
    public ApplicatorData cycle(int dir) {
        int n = blocks.size() + 1, cur = selected == RANDOM ? blocks.size() : selected;
        int next = Math.floorMod(cur + dir, n);
        return new ApplicatorData(blocks, next == blocks.size() ? RANDOM : next, name, link);
    }

    public Optional<Item> selectedBlock() {
        return selected >= 0 && selected < blocks.size() ? Optional.of(blocks.get(selected)) : Optional.empty();
    }
}
