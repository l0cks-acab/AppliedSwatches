package com.lach.appliedswatches.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lach.appliedswatches.AppliedSwatches;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
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
        run(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                return TinyFileDialogs.tinyfd_saveFileDialog(Component.translatable("gui.appliedswatches.save_title").getString(),
                        suggested.toString(), filter(stack), "JSON");
            }
        }, then);
    }

    public static void open(Path dir, Consumer<Path> then) {
        run(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                return TinyFileDialogs.tinyfd_openFileDialog(Component.translatable("gui.appliedswatches.open_title").getString(),
                        // A file pattern (not the bare folder) opens inside the folder on every platform.
                        dir.resolve("*.json").toString(), filter(stack), "JSON", false);
            }
        }, then);
    }

    private static PointerBuffer filter(MemoryStack stack) {
        return stack.mallocPointer(1).put(stack.UTF8("*.json")).flip();
    }

    private interface Dialog {
        String show();
    }

    private static void run(Dialog dialog, Consumer<Path> then) {
        Thread t = new Thread(() -> {
            String chosen = null;
            try {
                chosen = dialog.show();
            } catch (Throwable e) {
                LOGGER.warn("File dialog failed", e);
            }
            if (chosen == null || chosen.isBlank()) return;
            Path p = Path.of(chosen);
            Minecraft.getInstance().execute(() -> then.accept(p));
        }, "AppliedSwatches file dialog");
        t.setDaemon(true);
        t.start();
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
        if (p != null) p.displayClientMessage(Component.translatable(key, args), true);
    }
}
