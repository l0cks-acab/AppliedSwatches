package com.lach.appliedswatches;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handheld placer. A terminal loads a palette into it and links it; right-click places the selected block straight
 * out of that terminal's ME network, sneak + right-click or sneak + scroll changes the selection.
 */
public class ApplicatorItem extends Item {
    public ApplicatorItem(Properties props) {
        super(props);
    }

    public static ApplicatorData data(ItemStack stack) {
        return stack.getOrDefault(AppliedSwatches.APPLICATOR_DATA.get(), ApplicatorData.EMPTY);
    }

    /** Moves the selection and tells the player what is selected now. */
    public static void cycle(Player player, ItemStack stack, int dir) {
        ApplicatorData d = data(stack);
        if (d.blocks().isEmpty()) {
            player.displayClientMessage(Component.translatable("item.appliedswatches.swatch_applicator.empty").withStyle(ChatFormatting.RED), true);
            return;
        }
        d = d.cycle(dir);
        stack.set(AppliedSwatches.APPLICATOR_DATA.get(), d);
        Component what = d.selectedBlock().<Component>map(i -> i.getDescription())
                .orElse(Component.translatable("item.appliedswatches.swatch_applicator.random"));
        String pos = d.selected() == ApplicatorData.RANDOM ? "*" : (d.selected() + 1) + "/" + d.blocks().size();
        player.displayClientMessage(Component.translatable("item.appliedswatches.swatch_applicator.selected", what, pos), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) cycle(player, stack, 1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (!ctx.getLevel().isClientSide) cycle(player, ctx.getItemInHand(), 1);
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        return place((ServerPlayer) player, ctx);
    }

    private static InteractionResult place(ServerPlayer player, UseOnContext ctx) {
        ApplicatorData d = data(ctx.getItemInHand());
        if (d.blocks().isEmpty()) return fail(player, "empty");
        SwatchTerminalBlockEntity terminal = linkedTerminal(player, d);
        if (terminal == null) return InteractionResult.FAIL;
        IGrid grid = terminal.managedNode().isActive() ? terminal.managedNode().getGrid() : null;
        if (grid == null) return fail(player, "offline");

        List<Item> order = new ArrayList<>();
        d.selectedBlock().ifPresentOrElse(order::add, () -> {
            order.addAll(d.blocks());
            Collections.shuffle(order);
        });
        var storage = grid.getStorageService().getInventory();
        var energy = grid.getEnergyService();
        var src = IActionSource.ofPlayer(player, terminal);
        BlockHitResult hit = new BlockHitResult(ctx.getClickLocation(), ctx.getClickedFace(), ctx.getClickedPos(), ctx.isInside());
        for (Item item : order) {
            if (!(item instanceof BlockItem block)) continue;
            AEItemKey key = AEItemKey.of(item);
            BlockPlaceContext place = new BlockPlaceContext(ctx.getLevel(), player, ctx.getHand(), key.toStack(1), hit);
            if (!place.canPlace()) return InteractionResult.FAIL;
            // Check the simulated extraction first so a failed placement never has to put the block back.
            if (StorageHelper.poweredExtraction(energy, storage, key, 1, src, Actionable.SIMULATE) < 1) continue;
            if (!block.place(place).consumesAction()) return InteractionResult.FAIL;
            StorageHelper.poweredExtraction(energy, storage, key, 1, src);
            return InteractionResult.CONSUME;
        }
        return fail(player, "none_stored");
    }

    private static @Nullable SwatchTerminalBlockEntity linkedTerminal(ServerPlayer player, ApplicatorData d) {
        if (d.link().isEmpty()) {
            fail(player, "unlinked");
            return null;
        }
        GlobalPos link = d.link().get();
        int range = Config.APPLICATOR_RANGE.get();
        if (!link.dimension().equals(player.level().dimension())
                || range > 0 && player.blockPosition().distSqr(link.pos()) > (double) range * range) {
            fail(player, "out_of_range");
            return null;
        }
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(link.pos()) || !(level.getBlockEntity(link.pos()) instanceof SwatchTerminalBlockEntity be)) {
            fail(player, "unlinked");
            return null;
        }
        return be;
    }

    private static InteractionResult fail(Player player, String why) {
        player.displayClientMessage(Component.translatable("item.appliedswatches.swatch_applicator." + why).withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
    }

    @Override
    public Component getName(ItemStack stack) {
        ApplicatorData d = data(stack);
        if (d.blocks().isEmpty()) return super.getName(stack);
        Component what = d.selectedBlock().<Component>map(Item::getDescription)
                .orElse(Component.translatable("item.appliedswatches.swatch_applicator.random"));
        return Component.translatable("item.appliedswatches.swatch_applicator.named", super.getName(stack), what);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        ApplicatorData d = data(stack);
        if (d.blocks().isEmpty()) {
            tip.add(Component.translatable("item.appliedswatches.swatch_applicator.tip").withStyle(ChatFormatting.GRAY));
            return;
        }
        tip.add(Component.translatable("item.appliedswatches.swatch_applicator.palette", d.name(), d.blocks().size()).withStyle(ChatFormatting.GRAY));
        d.link().ifPresent(l -> tip.add(Component.translatable("item.appliedswatches.swatch_applicator.linked",
                l.pos().getX(), l.pos().getY(), l.pos().getZ()).withStyle(ChatFormatting.DARK_GRAY)));
        tip.add(Component.translatable("item.appliedswatches.swatch_applicator.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
