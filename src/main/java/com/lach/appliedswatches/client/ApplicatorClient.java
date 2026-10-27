package com.lach.appliedswatches.client;

import com.lach.appliedswatches.AppliedSwatches;
import com.lach.appliedswatches.ApplicatorItem;
import com.lach.appliedswatches.net.CycleApplicatorPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Applicator rendering and sneak + scroll selection. */
@EventBusSubscriber(modid = AppliedSwatches.MODID, value = Dist.CLIENT)
public final class ApplicatorClient {
    private ApplicatorClient() {
    }

    @SubscribeEvent
    static void models(ModelEvent.RegisterAdditional event) {
        event.register(ApplicatorRenderer.BASE);
    }

    @SubscribeEvent
    static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private ApplicatorRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new ApplicatorRenderer();
                return renderer;
            }
        }, AppliedSwatches.APPLICATOR.get());
    }

    @SubscribeEvent
    static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isShiftKeyDown() || event.getScrollDeltaY() == 0) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof ApplicatorItem)) return;
        PacketDistributor.sendToServer(new CycleApplicatorPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
        event.setCanceled(true);
    }
}
