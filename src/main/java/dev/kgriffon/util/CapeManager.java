package dev.kgriffon.util;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.kgriffon.MoreCapes;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CapeManager {

    private static final List<String> CAPES = new ArrayList<>();

    public static void reload() {
        try {
            CAPES.clear();
            Files.createDirectories(basePath().resolve(".cache"));
            try (Stream<Path> stream = Files.walk(basePath())) {
                stream.forEach(path -> {
                    Path relativePath = basePath().relativize(path);
                    if (Files.isRegularFile(basePath().resolve(relativePath)) && !relativePath.startsWith(".cache")) {
                        String pathId = relativePath.toString();
                        if (pathId.endsWith(".png")) {
                            CAPES.add(pathId.substring(0, pathId.length() - 4).replace("\\", "/"));
                        }
                    }
                });
            }

        } catch (IOException e) {
            MoreCapes.LOGGER.error("Error while initializing {}", e.getMessage());
        }
    }

    public static boolean contains(String id) {
        return CAPES.contains(id);
    }

    public static Path getCapePath(String id) {
        return basePath().resolve(id + ".png");
    }

    public static SuggestionProvider<FabricClientCommandSource> getCapes() {
        return (context, builder) -> {
            String remaining = builder.getRemaining().toLowerCase();

            CAPES.stream()
                    .filter(id -> id.toLowerCase().startsWith(remaining))
                    .forEach(builder::suggest);

            return builder.buildFuture();
        };
    }

    public static Path getOutputPath() {
        return basePath().resolve(".cache").resolve("skin.png");
    }

    private static Path basePath() {
        return FabricLoader.getInstance().getConfigDir().resolve(MoreCapes.MOD_ID);
    }
}
