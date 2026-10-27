package com.lach.appliedswatches.client;

import com.lach.appliedswatches.AppliedSwatches;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AppliedSwatches.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    static void screens(RegisterMenuScreensEvent event) {
        event.register(AppliedSwatches.TERMINAL_MENU.get(), SwatchScreen::new);
        event.register(AppliedSwatches.THEME_MENU.get(), ThemeScreen::new);
    }

    @SubscribeEvent
    static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) rm -> BlockColorCache.clear());
    }
}
