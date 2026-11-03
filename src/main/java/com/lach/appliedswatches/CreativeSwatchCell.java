package com.lach.appliedswatches;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Test cell: holds an endless supply of every block item in the game. Extraction never runs out, inserted blocks are voided. */
public final class CreativeSwatchCell implements StorageCell {
    public static final long SHOWN = 1_000_000;
    public static final ICellHandler HANDLER = new ICellHandler() {
        @Override
        public boolean isCell(ItemStack stack) {
            return stack.is(AppliedSwatches.CREATIVE_CELL.get());
        }

        @Override
        public StorageCell getCellInventory(ItemStack stack, ISaveProvider host) {
            return isCell(stack) ? INSTANCE : null;
        }
    };
    private static final CreativeSwatchCell INSTANCE = new CreativeSwatchCell();
    private static List<AEItemKey> keys;

    private static List<AEItemKey> keys() {
        if (keys == null) {
            List<AEItemKey> k = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof BlockItem) k.add(AEItemKey.of(item));
            }
            keys = k;
        }
        return keys;
    }

    private static boolean covers(AEKey what) {
        return what instanceof AEItemKey k && !k.hasComponents() && k.getItem() instanceof BlockItem;
    }

    @Override
    public CellState getStatus() {
        return CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return 0;
    }

    @Override
    public void persist() {
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        return covers(what) ? amount : 0;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        return covers(what) ? amount : 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (AEItemKey k : keys()) out.add(k, SHOWN);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("item.appliedswatches.creative_swatch_cell");
    }
}
