package com.lach.appliedswatches;

import appeng.api.AECapabilities;
import com.lach.appliedswatches.net.CandidatesPayload;
import com.lach.appliedswatches.net.ExportPayload;
import com.lach.appliedswatches.net.RefreshPayload;
import com.lach.appliedswatches.net.ReturnPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(AppliedSwatches.MODID)
public class AppliedSwatches {
    public static final String MODID = "appliedswatches";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredBlock<SwatchTerminalBlock> TERMINAL = BLOCKS.registerBlock("swatch_terminal", SwatchTerminalBlock::new, SwatchTerminalBlock.PROPERTIES);
    public static final DeferredBlock<SwatchTerminalBlock> THEME_TERMINAL = BLOCKS.registerBlock("theme_terminal", SwatchTerminalBlock::new, SwatchTerminalBlock.PROPERTIES);
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = ITEMS.registerSimpleBlockItem(TERMINAL, new Item.Properties());
    public static final DeferredItem<BlockItem> THEME_TERMINAL_ITEM = ITEMS.registerSimpleBlockItem(THEME_TERMINAL, new Item.Properties());
    /** Both terminals share one block entity; the block decides which screen opens. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwatchTerminalBlockEntity>> TERMINAL_BE = BLOCK_ENTITIES.register("swatch_terminal",
            () -> BlockEntityType.Builder.of(SwatchTerminalBlockEntity::new, TERMINAL.get(), THEME_TERMINAL.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SwatchMenu>> TERMINAL_MENU = MENUS.register("swatch_terminal",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new SwatchMenu(AppliedSwatches.TERMINAL_MENU.get(), id, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<SwatchMenu>> THEME_MENU = MENUS.register("theme_terminal",
            () -> IMenuTypeExtension.create((id, inv, buf) -> new SwatchMenu(AppliedSwatches.THEME_MENU.get(), id, inv)));

    public AppliedSwatches(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(AppliedSwatches::registerCaps);
        modBus.addListener(AppliedSwatches::registerPayloads);
        modBus.addListener(AppliedSwatches::creativeTabs);
        LOGGER.info("{} loaded", MODID);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerCaps(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, TERMINAL_BE.get(), (be, ctx) -> be);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TERMINAL_BE.get(), (be, side) -> be.output());
    }

    private static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TERMINAL_ITEM.get());
            event.accept(THEME_TERMINAL_ITEM.get());
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(CandidatesPayload.TYPE, CandidatesPayload.STREAM_CODEC, (p, ctx) -> ClientBridge.candidates(p));
        r.playToServer(RefreshPayload.TYPE, RefreshPayload.STREAM_CODEC, (p, ctx) -> {
            SwatchMenu menu = openMenu(ctx);
            if (menu != null) menu.sendCandidates();
        });
        r.playToServer(ReturnPayload.TYPE, ReturnPayload.STREAM_CODEC, (p, ctx) -> {
            SwatchMenu menu = openMenu(ctx);
            if (menu != null) {
                menu.returnAll();
                menu.sendCandidates();
            }
        });
        r.playToServer(ExportPayload.TYPE, ExportPayload.STREAM_CODEC, (p, ctx) -> {
            SwatchMenu menu = openMenu(ctx);
            if (menu != null) {
                menu.export(p.items(), p.perItem());
                menu.sendCandidates();
            }
        });
    }

    private static SwatchMenu openMenu(IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.containerMenu instanceof SwatchMenu m && m.stillValid(sp)) return m;
        return null;
    }
}
