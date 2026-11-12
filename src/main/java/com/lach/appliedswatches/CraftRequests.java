package com.lach.appliedswatches;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

/** Crafting jobs asked for from a terminal: calculated off-thread by AE2, submitted on a later server tick. */
public final class CraftRequests {
    private record Job(Batch batch, Item item, Future<ICraftingPlan> plan) {
    }

    private static final class Batch {
        final ServerPlayer player;
        final IGrid grid;
        final IActionSource src;
        int pending, started, failed;

        Batch(ServerPlayer player, IGrid grid, IActionSource src) {
            this.player = player;
            this.grid = grid;
            this.src = src;
        }
    }

    private static final List<Job> JOBS = new ArrayList<>();

    private CraftRequests() {
    }

    public static void request(ServerPlayer player, IGrid grid, IActionSource src, List<Item> items, int amount) {
        var crafting = grid.getCraftingService();
        Batch batch = new Batch(player, grid, src);
        for (Item item : items) {
            if (!(item instanceof BlockItem)) continue;
            AEItemKey key = AEItemKey.of(item);
            if (!crafting.isCraftable(key)) {
                batch.failed++;
                continue;
            }
            batch.pending++;
            JOBS.add(new Job(batch, item, crafting.beginCraftingCalculation(player.level(), () -> src, key, amount, CalculationStrategy.CRAFT_LESS)));
        }
        if (batch.pending == 0) report(batch);
    }

    /** Drops jobs left over from a server that has stopped, so they can't hold on to its grids and players. */
    public static void clear() {
        JOBS.clear();
    }

    public static void tick() {
        for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
            Job job = it.next();
            if (!job.plan.isDone()) continue;
            it.remove();
            Batch b = job.batch;
            try {
                ICraftingPlan plan = job.plan.get();
                boolean ok = !plan.simulation() && plan.finalOutput().amount() > 0
                        && b.grid.getCraftingService().submitJob(plan, null, null, true, b.src).successful();
                if (ok) b.started++;
                else b.failed++;
            } catch (Exception e) {
                b.failed++;
            }
            if (--b.pending == 0) report(b);
        }
    }

    private static void report(Batch b) {
        if (b.player.hasDisconnected()) return;
        if (b.started > 0)
            b.player.sendSystemMessage(Component.translatable("gui.appliedswatches.craft_started", b.started).withStyle(ChatFormatting.GREEN));
        if (b.failed > 0)
            b.player.sendSystemMessage(Component.translatable("gui.appliedswatches.craft_failed", b.failed).withStyle(ChatFormatting.RED));
    }
}
