package com.lach.appliedswatches;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** The terminal's 27 output slots. */
public class OutputHandler extends ItemStacksResourceHandler {
    private final Runnable onChange;

    public OutputHandler(int size, Runnable onChange) {
        super(size);
        this.onChange = onChange;
    }

    public ItemStack getStack(int i) {
        return stacks.get(i);
    }

    /** Inserts as much as fits, stacking onto matching slots first. Returns the amount inserted. */
    public int insertStacked(ItemResource res, int amount, boolean simulate) {
        try (var tx = Transaction.openRoot()) {
            int n = ResourceHandlerUtil.insertStacking(this, res, amount, tx);
            if (!simulate) tx.commit();
            return n;
        }
    }

    public int extract(int i, int amount) {
        ItemResource res = getResource(i);
        if (res.isEmpty()) return 0;
        try (var tx = Transaction.openRoot()) {
            int n = extract(i, res, amount, tx);
            tx.commit();
            return n;
        }
    }

    @Override
    protected void onContentsChanged(int index, ItemStack previous) {
        onChange.run();
    }
}
