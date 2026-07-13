package dev.kgriffon.capes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.kgriffon.capes.MoreCapes;
import dev.kgriffon.capes.util.CapeCache;
import dev.kgriffon.capes.util.CapeManager;
import dev.kgriffon.capes.util.MojangApi;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.PlayerModelType;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

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
                        .then(literal("debug")
                                .executes(context -> debug())
                        )
                )
        );
    }

    private static int reload() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        CapeManager.reload();
        if (player != null) player.sendSystemMessage(Component.literal("The cape folder has been reloaded.").withStyle(ChatFormatting.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int updateCape(String capeId) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player != null) {

            player.sendSystemMessage(Component.literal("Applying the cape...").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));

            if (!CapeManager.contains(capeId)) {
                player.sendSystemMessage(Component.literal("This cape doesn't exist.").withColor(CommonColors.SOFT_RED));
            }

            Path capeTexture = CapeManager.getCapePath(capeId);

            CompletableFuture.runAsync(() -> {
                String capeHash = MojangApi.uploadSkin(client.getUser().getAccessToken(), capeTexture.toFile(), player.getSkin().model() == PlayerModelType.SLIM ? "slim" : "classic");
                if (capeHash != null) {
                    try {
                        Thread.sleep(2000);
                        client.execute(() -> {
                            String url = "http://textures.minecraft.net/texture/" + capeHash;
                            player.sendSystemMessage(Component.literal(url).withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE).withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))));
                        });
                        updateCapeUrl(capeHash);
                    } catch (InterruptedException e) {
                        MoreCapes.LOGGER.info("An error has occurred {}", e.getMessage());
                    }
                } else {
                    client.execute(() -> player.sendSystemMessage(Component.literal("An error has occurred.").withColor(CommonColors.SOFT_RED)));
                }
            });
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int updateCapeUrl(String capeUrl) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

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
                    MojangApi.uploadSkin(client.getUser().getAccessToken(), CapeManager.getOutputPath().toFile(), player.getSkin().model() == PlayerModelType.SLIM ? "slim" : "classic");
                    client.execute(() -> {
                        MutableComponent message = Component.literal("The cape has been successfully applied.")
                                .withStyle(style -> Style.EMPTY
                                        .withColor(CommonColors.GREEN)
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("If you are playing solo, you must restart your game. If you are playing multiplayer, simply reconnect to the server and the cape will be updated for all players.")
                                                .withColor(CommonColors.LIGHT_GRAY)))
                                );
                        player.sendSystemMessage(message);
                    });
                } catch (Exception e) {
                    MoreCapes.LOGGER.info("An error has occurred {}", e.getMessage());
                    client.execute(() -> player.sendSystemMessage(Component.literal("An error has occurred.").withColor(CommonColors.SOFT_RED)));
                }
            });
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reset() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player != null) {

            player.sendSystemMessage(Component.literal("Resetting the cape...").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));

            Identifier texturePath = player.getSkin().body().texturePath();
            String hash = texturePath.getPath().split("/")[1];
            Path path = CapeCache.getAssetsCache().resolve(hash.length() > 2 ? hash.substring(0, 2) : "xx").resolve(hash);
            File originalSkin = path.toFile();

            CompletableFuture.runAsync(() -> {
                try {
                    BufferedImage image = ImageIO.read(originalSkin);
                    image.setRGB(0, 0, (int) Long.parseLong("00000000", 16));
                    ImageIO.write(image, "png", CapeManager.getOutputPath().toFile());
                    MojangApi.uploadSkin(client.getUser().getAccessToken(), CapeManager.getOutputPath().toFile(), player.getSkin().model() == PlayerModelType.SLIM ? "slim" : "classic");
                    client.execute(() -> {
                        MutableComponent message = Component.literal("The cape has been successfully reset.")
                                .withStyle(style -> Style.EMPTY
                                        .withColor(CommonColors.GREEN)
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("If you are playing solo, you must restart your game. If you are playing multiplayer, simply reconnect to the server and the cape will be updated for all players.")
                                                .withColor(CommonColors.LIGHT_GRAY)))
                                );
                        player.sendSystemMessage(message);
                    });
                } catch (Exception e) {
                    MoreCapes.LOGGER.info("An error has occurred {}", e.getMessage());
                    client.execute(() -> player.sendSystemMessage(Component.literal("An error has occurred.").withColor(CommonColors.SOFT_RED)));
                }

            });
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int debug() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player != null) {
            CapeCache.getAll().forEach((skinHash, capeHash) -> {
                if (capeHash != null) {
                    MutableComponent skinText = Component.literal(skinHash).withStyle(style -> style
                            .withColor(ChatFormatting.GRAY)
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create("http://textures.minecraft.net/texture/" + skinHash))));
                    MutableComponent capeText = Component.literal(capeHash).withStyle(style -> style
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create("http://textures.minecraft.net/texture/" + capeHash))));
                    player.sendSystemMessage(Component.literal("---------").withStyle(ChatFormatting.DARK_RED));
                    player.sendSystemMessage(skinText.append(" -> ").append(capeText));
                    player.sendSystemMessage(Component.literal("---------").withStyle(ChatFormatting.DARK_RED));
                }
            });
        }

        return Command.SINGLE_SUCCESS;
    }
}
