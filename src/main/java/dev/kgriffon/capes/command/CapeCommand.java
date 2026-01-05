package dev.kgriffon.capes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.kgriffon.capes.MoreCapes;
import dev.kgriffon.capes.util.CapeCache;
import dev.kgriffon.capes.util.CapeManager;
import dev.kgriffon.capes.util.MojangApi;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.text.*;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CapeCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("cape")
                        .then(literal("reload")
                                .executes(context -> reload()))
                        .then(literal("set")
                                .then(literal("file")
                                        .then(argument("id", StringArgumentType.greedyString())
                                                .suggests(CapeManager.getCapes())
                                                .executes(context -> updateCape(StringArgumentType.getString(context, "id")))
                                        )
                                )
                                .then(literal("url")
                                        .then(argument("url", StringArgumentType.greedyString())
                                                .executes(context -> updateCapeUrl(StringArgumentType.getString(context, "url")))
                                        )
                                )
                        )
                        .then(literal("reset")
                                .executes(context -> reset())
                        )
                )
        );
    }

    private static int reload() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        CapeManager.reload();
        if (player != null) player.sendMessage(Text.literal("The cape folder has been reloaded.").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int updateCape(String capeId) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player != null) {

            player.sendMessage(Text.literal("Applying the cape...").formatted(Formatting.ITALIC, Formatting.GRAY), false);

            if (!CapeManager.contains(capeId)) {
                player.sendMessage(Text.literal("This cape doesn't exist.").withColor(Colors.LIGHT_RED), false);
            }

            Path capeTexture = CapeManager.getCapePath(capeId);

            CompletableFuture.runAsync(() -> {
                String capeHash = MojangApi.uploadSkin(client.getSession().getAccessToken(), capeTexture.toFile(), player.getSkin().model() == PlayerSkinType.SLIM ? "slim" : "classic");
                if (capeHash != null) {
                    try {
                        Thread.sleep(2000);
                        client.execute(() -> {
                            String url = "http://textures.minecraft.net/texture/" + capeHash;
                            player.sendMessage(Text.literal(url).formatted(Formatting.YELLOW, Formatting.UNDERLINE).styled(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))), false);
                        });
                        updateCapeUrl(capeHash);
                    } catch (InterruptedException e) {
                        MoreCapes.LOGGER.info("An error has occurred {}", e.getMessage());
                    }
                } else {
                    client.execute(() -> player.sendMessage(Text.literal("An error has occurred.").withColor(Colors.LIGHT_RED), false));
                }
            });
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int updateCapeUrl(String capeUrl) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player != null) {
            Identifier texturePath = player.getSkin().body().texturePath();
            String hash = texturePath.getPath().split("/")[1];
            Path path = CapeCache.getAssetsCache().resolve(hash.length() > 2 ? hash.substring(0, 2) : "xx").resolve(hash);
            File originalSkin = path.toFile();

            String[] url = capeUrl.split("/");
            String capeHash = url[url.length - 1];
            MoreCapes.LOGGER.info("Set cape {}", capeHash);

            CompletableFuture.runAsync(() -> {
                try {
                    String finalHash = String.format("%64s", capeHash).replace(' ', '0');
                    BufferedImage image = ImageIO.read(originalSkin);

                    image.setRGB(0, 0, (int) Long.parseLong("CAFEBABE", 16));
                    for (int i = 0; i < 8; i++) {
                        image.setRGB(i, 1, (int) Long.parseLong(finalHash.substring(i * 8, i * 8 + 8), 16));
                    }

                    ImageIO.write(image, "png", CapeManager.getOutputPath().toFile());
                    MojangApi.uploadSkin(client.getSession().getAccessToken(), CapeManager.getOutputPath().toFile(), player.getSkin().model() == PlayerSkinType.SLIM ? "slim" : "classic");
                    client.execute(() -> {
                        MutableText message = Text.literal("The cape has been successfully applied.")
                                .styled(style -> Style.EMPTY
                                        .withColor(Colors.GREEN)
                                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("If you are playing solo, you must restart your game. If you are playing multiplayer, simply reconnect to the server and the cape will be updated for all players.")
                                                .withColor(Colors.LIGHT_GRAY)))
                                );
                        player.sendMessage(message, false);
                    });
                } catch (Exception e) {
                    MoreCapes.LOGGER.info("An error has occurred {}", e.getMessage());
                    client.execute(() -> player.sendMessage(Text.literal("An error has occurred.").withColor(Colors.LIGHT_RED), false));
                }
            });
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reset() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player != null) {

            player.sendMessage(Text.literal("Resetting the cape...").formatted(Formatting.ITALIC, Formatting.GRAY), false);

            Identifier texturePath = player.getSkin().body().texturePath();
            String hash = texturePath.getPath().split("/")[1];
            Path path = CapeCache.getAssetsCache().resolve(hash.length() > 2 ? hash.substring(0, 2) : "xx").resolve(hash);
            File originalSkin = path.toFile();

            CompletableFuture.runAsync(() -> {
                try {
                    BufferedImage image = ImageIO.read(originalSkin);
                    image.setRGB(0, 0, (int) Long.parseLong("00000000", 16));
                    ImageIO.write(image, "png", CapeManager.getOutputPath().toFile());
                    MojangApi.uploadSkin(client.getSession().getAccessToken(), CapeManager.getOutputPath().toFile(), player.getSkin().model() == PlayerSkinType.SLIM ? "slim" : "classic");
                    client.execute(() -> {
                        MutableText message = Text.literal("The cape has been successfully reset.")
                                .styled(style -> Style.EMPTY
                                        .withColor(Colors.GREEN)
                                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("If you are playing solo, you must restart your game. If you are playing multiplayer, simply reconnect to the server and the cape will be updated for all players.")
                                                .withColor(Colors.LIGHT_GRAY)))
                                );
                        player.sendMessage(message, false);
                    });
                } catch (Exception e) {
                    MoreCapes.LOGGER.info("An error has occurred {}", e.getMessage());
                    client.execute(() -> player.sendMessage(Text.literal("An error has occurred.").withColor(Colors.LIGHT_RED), false));
                }

            });
        }
        return Command.SINGLE_SUCCESS;
    }
}
