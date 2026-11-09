package com.lach.appliedswatches.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lach.appliedswatches.AppliedSwatches;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Native open/save dialogs for .json files, run off the render thread, plus JSON read/write. */
public final class FileDialogs {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private FileDialogs() {
    }

    /** {@code <game dir>/appliedswatches/<sub>}, created if missing. */
    public static Path dir(String sub) {
        Path p = FMLPaths.GAMEDIR.get().resolve(AppliedSwatches.MODID).resolve(sub);
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            LOGGER.warn("Could not create {}", p, e);
        }
        return p;
    }

    public static void save(Path suggested, Consumer<Path> then) {
        show(true, suggested.toString(), then);
    }

    public static void open(Path dir, Consumer<Path> then) {
        show(false, dir.toString() + java.io.File.separator, then);
    }

    /** SDL file dialog; opened on the render thread, result handed back on it. */
    private static void show(boolean save, String location, Consumer<Path> then) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            ByteBuffer name = MemoryUtil.memUTF8("JSON");
            ByteBuffer pattern = MemoryUtil.memUTF8("json");
            SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.calloc(1);
            filters.get(0).name(name).pattern(pattern);
            SDL_DialogFileCallback[] cb = new SDL_DialogFileCallback[1];
            cb[0] = SDL_DialogFileCallback.create((userdata, list, filter) -> {
                String chosen = null;
                if (list != 0L) {
                    long first = MemoryUtil.memGetAddress(list);
                    if (first != 0L) chosen = MemoryUtil.memUTF8(first);
                }
                String c = chosen;
                mc.execute(() -> {
                    cb[0].free();
                    filters.free();
                    MemoryUtil.memFree(name);
                    MemoryUtil.memFree(pattern);
                    if (c != null && !c.isBlank()) then.accept(Path.of(c));
                });
            });
            try {
                long window = mc.getWindow().handle();
                if (save) SDLDialog.SDL_ShowSaveFileDialog(cb[0], 0L, window, filters, location);
                else SDLDialog.SDL_ShowOpenFileDialog(cb[0], 0L, window, filters, location, false);
            } catch (Throwable e) {
                LOGGER.warn("File dialog failed", e);
            }
        });
    }

    public static JsonObject read(Path p) throws IOException {
        try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            return GSON.fromJson(r, JsonObject.class);
        }
    }

    public static void write(Path p, JsonObject json) throws IOException {
        if (!p.getFileName().toString().toLowerCase().endsWith(".json")) p = p.resolveSibling(p.getFileName() + ".json");
        Files.writeString(p, GSON.toJson(json), StandardCharsets.UTF_8);
    }

    /** Tells the player how a file action went. */
    public static void toast(String key, Object... args) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.sendOverlayMessage(Component.translatable(key, args));
    }
}
