package com.lach.appliedswatches.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** The player's own themes from the game folder (newest first), then built-in ones from resource packs ({@code assets/<ns>/themes/*.json}). */
public final class ThemeManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ThemeManager() {
    }

    public static Path userDir() {
        return FileDialogs.dir("themes");
    }

    public static List<Theme> load() {
        List<Theme> builtin = new ArrayList<>();
        Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> found =
                Minecraft.getInstance().getResourceManager().listResources("themes", rl -> rl.getPath().endsWith(".json"));
        for (var e : found.entrySet()) {
            try (Reader r = e.getValue().openAsReader()) {
                String file = e.getKey().getPath();
                builtin.add(Theme.fromJson(FileDialogs.GSON.fromJson(r, com.google.gson.JsonObject.class),
                        file.substring(file.lastIndexOf('/') + 1, file.length() - 5), true));
            } catch (Exception ex) {
                LOGGER.warn("Bad theme {}", e.getKey(), ex);
            }
        }
        builtin.sort(Comparator.comparing(t -> t.name().toLowerCase()));
        List<Theme> user = new ArrayList<>();
        try (Stream<Path> files = Files.list(userDir())) {
            // Newest first, so a just-imported or just-saved theme is at the top of the list.
            for (Path p : files.filter(f -> f.toString().toLowerCase().endsWith(".json"))
                    .sorted(Comparator.comparingLong(ThemeManager::modified).reversed()).toList()) {
                try {
                    String n = p.getFileName().toString();
                    user.add(Theme.fromJson(FileDialogs.read(p), n.substring(0, n.length() - 5), false));
                } catch (Exception ex) {
                    LOGGER.warn("Bad theme file {}", p, ex);
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not list {}", userDir(), ex);
        }
        user.addAll(builtin);
        return user;
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }
}
