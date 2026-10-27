package com.lach.appliedswatches;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import com.lach.appliedswatches.net.CandidatesPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SwatchMenu extends AbstractContainerMenu {
    /** Most block types sent to the client in one listing. */
    public static final int MAX_CANDIDATES = 4096;
    public static final int OUT_X = 188, OUT_Y = 144, INV_X = 8, INV_Y = 144;

    private final @Nullable SwatchTerminalBlockEntity be;
    private final Player player;
    private final ContainerLevelAccess access;
    private final DataSlot online;

    /** Client side. */
    public SwatchMenu(MenuType<?> type, int id, Inventory inv) {
        this(type, id, inv, null, new ItemStackHandler(SwatchTerminalBlockEntity.SLOTS), ContainerLevelAccess.NULL, DataSlot.standalone());
    }

    /** Server side. */
    public SwatchMenu(MenuType<?> type, int id, Inventory inv, SwatchTerminalBlockEntity be) {
        this(type, id, inv, be, be.output(), ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), new DataSlot() {
            @Override
            public int get() {
                return be.managedNode().isActive() ? 1 : 0;
            }

            @Override
            public void set(int value) {
            }
        });
    }

    private SwatchMenu(MenuType<?> type, int id, Inventory inv, @Nullable SwatchTerminalBlockEntity be, ItemStackHandler output, ContainerLevelAccess access, DataSlot online) {
        super(type, id);
        this.be = be;
        this.player = inv.player;
        this.access = access;
        this.online = addDataSlot(online);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new SlotItemHandler(output, c + r * 9, OUT_X + c * 18, OUT_Y + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, INV_X + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, INV_X + c * 18, INV_Y + 58));
    }

    /** Where the terminal is, for linking an applicator; null on the client. */
    public @Nullable net.minecraft.core.GlobalPos terminalPos() {
        return be == null || be.getLevel() == null ? null : net.minecraft.core.GlobalPos.of(be.getLevel().dimension(), be.getBlockPos());
    }

    public boolean online() {
        return online.get() == 1;
    }

    private @Nullable IGrid grid() {
        return be != null && be.managedNode().isActive() ? be.managedNode().getGrid() : null;
    }

    /** Sends the block items the network holds to this menu's player. */
    public void sendCandidates() {
        if (!(player instanceof ServerPlayer sp)) return;
        List<Item> items = new ArrayList<>();
        List<Long> amounts = new ArrayList<>();
        IGrid grid = grid();
        if (grid != null) {
            for (var e : grid.getStorageService().getCachedInventory()) {
                if (items.size() >= MAX_CANDIDATES) break;
                if (e.getKey() instanceof AEItemKey key && !key.hasComponents() && key.getItem() instanceof BlockItem && e.getLongValue() > 0) {
                    items.add(key.getItem());
                    amounts.add(e.getLongValue());
                }
            }
        }
        PacketDistributor.sendToPlayer(sp, new CandidatesPayload(grid != null, items, amounts.stream().mapToLong(Long::longValue).toArray()));
    }

    /** Pulls up to {@code perItem} of each item, in order, out of the network into the output slots. */
    public void export(List<Item> items, int perItem) {
        IGrid grid = grid();
        if (grid == null || be == null) return;
        var storage = grid.getStorageService().getInventory();
        var energy = grid.getEnergyService();
        var src = IActionSource.ofPlayer(player, be);
        ItemStackHandler out = be.output();
        for (Item item : items) {
            if (!(item instanceof BlockItem)) continue;
            AEItemKey key = AEItemKey.of(item);
            int want = Math.min(perItem, item.getDefaultMaxStackSize() * out.getSlots());
            ItemStack left = ItemHandlerHelper.insertItemStacked(out, key.toStack(want), true);
            int fits = want - left.getCount();
            if (fits <= 0) continue;
            long got = StorageHelper.poweredExtraction(energy, storage, key, fits, src);
            if (got <= 0) continue;
            ItemStack rest = ItemHandlerHelper.insertItemStacked(out, key.toStack((int) got), false);
            if (!rest.isEmpty()) StorageHelper.poweredInsert(energy, storage, key, rest.getCount(), src);
        }
    }

    /** Puts everything in the output slots back into the network. */
    public void returnAll() {
        IGrid grid = grid();
        if (grid == null || be == null) return;
        var storage = grid.getStorageService().getInventory();
        var energy = grid.getEnergyService();
        var src = IActionSource.ofPlayer(player, be);
        ItemStackHandler out = be.output();
        for (int i = 0; i < out.getSlots(); i++) {
            ItemStack s = out.getStackInSlot(i);
            AEItemKey key = AEItemKey.of(s);
            if (key == null) continue;
            long put = StorageHelper.poweredInsert(energy, storage, key, s.getCount(), src);
            if (put > 0) out.extractItem(i, (int) put, false);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        int out = SwatchTerminalBlockEntity.SLOTS;
        if (index < out) {
            if (!moveItemStackTo(stack, out, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, out, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof SwatchTerminalBlock
                && player.canInteractWithBlock(pos, 4.0), true);
    }
}
