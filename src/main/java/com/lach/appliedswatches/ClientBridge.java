package com.lach.appliedswatches;

import com.lach.appliedswatches.client.TerminalScreen;
import com.lach.appliedswatches.net.CandidatesPayload;
import net.minecraft.client.Minecraft;

/** Keeps client classes out of common code paths; only called on the client. */
final class ClientBridge {
    static void openApplicator(net.minecraft.world.InteractionHand hand) {
        Minecraft.getInstance().gui.setScreen(new com.lach.appliedswatches.client.ApplicatorScreen(hand));
    }

    static void candidates(CandidatesPayload p) {
        if (Minecraft.getInstance().gui.screen() instanceof TerminalScreen s) s.setCandidates(p);
    }
}
