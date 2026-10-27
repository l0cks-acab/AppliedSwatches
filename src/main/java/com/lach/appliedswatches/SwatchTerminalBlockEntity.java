package com.lach.appliedswatches;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class SwatchTerminalBlockEntity extends BlockEntity implements IInWorldGridNodeHost, IActionHost, MenuProvider {
    public static final int SLOTS = 27;

    private final IManagedGridNode node = GridHelper.createManagedNode(this, Listener.INSTANCE)
            .setFlags(GridFlags.REQUIRE_CHANNEL)
            .setInWorldNode(true)
            .setTagName("node");
    private final ItemStackHandler output = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public SwatchTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(AppliedSwatches.TERMINAL_BE.get(), pos, state);
        node.setVisualRepresentation(AppliedSwatches.TERMINAL_ITEM.get());
    }

    public ItemStackHandler output() {
        return output;
    }

    public IManagedGridNode managedNode() {
        return node;
    }

    public void setOwner(Player player) {
        node.setOwningPlayer(player);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) GridHelper.onFirstTick(this, be -> {
            be.node.setIdlePowerUsage(be.getBlockState().is(AppliedSwatches.THEME_TERMINAL.get())
                    ? Config.THEME_IDLE_POWER.get() : Config.SWATCH_IDLE_POWER.get());
            be.node.create(be.level, be.worldPosition);
        });
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        node.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        node.destroy();
    }

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        return node.getNode();
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        return node.getNode();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        node.saveToNBT(tag);
        tag.put("output", output.serializeNBT(regs));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        node.loadFromNBT(tag);
        if (tag.contains("output")) output.deserializeNBT(regs, tag.getCompound("output"));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        var type = getBlockState().is(AppliedSwatches.THEME_TERMINAL.get()) ? AppliedSwatches.THEME_MENU.get() : AppliedSwatches.TERMINAL_MENU.get();
        return new SwatchMenu(type, id, inv, this);
    }

    private enum Listener implements IGridNodeListener<SwatchTerminalBlockEntity> {
        INSTANCE;

        @Override
        public void onSaveChanges(SwatchTerminalBlockEntity be, IGridNode node) {
            be.setChanged();
        }
    }
}
