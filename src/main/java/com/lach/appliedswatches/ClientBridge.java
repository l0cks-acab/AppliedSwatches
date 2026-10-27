package com.lach.appliedswatches;

import com.lach.appliedswatches.client.TerminalScreen;
import com.lach.appliedswatches.net.CandidatesPayload;
import net.minecraft.client.Minecraft;

/** Keeps client classes out of common code paths; only called on the client. */
final class ClientBridge {
    static void candidates(CandidatesPayload p) {
        if (Minecraft.getInstance().screen instanceof TerminalScreen s) s.setCandidates(p);
    }
}
