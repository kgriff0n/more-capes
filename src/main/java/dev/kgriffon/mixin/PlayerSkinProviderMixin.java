package dev.kgriffon.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import dev.kgriffon.MoreCapes;
import dev.kgriffon.util.CapeCache;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.ApiServices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(PlayerSkinProvider.class)
public abstract class PlayerSkinProviderMixin {

    @Shadow
    abstract CompletableFuture<SkinTextures> fetchSkinTextures(UUID uuid, MinecraftProfileTextures textures);

    @Inject(at = @At("HEAD"), method = "<init>")
    private static void getAssetsCache(Path cacheDirectory, ApiServices apiServices, PlayerSkinTextureDownloader downloader, Executor executor, CallbackInfo ci) {
        CapeCache.setAssetsCache(cacheDirectory);
    }

    @ModifyExpressionValue(
            method = "fetchSkinTextures(Ljava/util/UUID;Lcom/mojang/authlib/minecraft/MinecraftProfileTextures;)Ljava/util/concurrent/CompletableFuture;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/authlib/minecraft/MinecraftProfileTextures;cape()Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;"
            )
    )
    private MinecraftProfileTexture replaceCape(
            MinecraftProfileTexture cape,
            UUID uuid,
            MinecraftProfileTextures textures
    ) {
        if (textures.skin() != null) {
            String skinHash = textures.skin().getHash();
            String capeHash = CapeCache.getCape(skinHash);

            if (capeHash != null) {
                MoreCapes.LOGGER.info("Apply the cape for {}", uuid);
                return new MinecraftProfileTexture(
                        "https://textures.minecraft.net/texture/" + capeHash,
                        Map.of()
                );
            }
        }

        return cape;
    }

    @ModifyReturnValue(
            at = @At("RETURN"),
            method = "fetchSkinTextures(Ljava/util/UUID;Lcom/mojang/authlib/minecraft/MinecraftProfileTextures;)Ljava/util/concurrent/CompletableFuture;"
    )
    private CompletableFuture<SkinTextures> fetchTextures(CompletableFuture<SkinTextures> original, UUID uuid, MinecraftProfileTextures textures) {

        return original.thenCompose(skinTextures -> {
            if (textures.skin() != null) {
                String skinHash = textures.skin().getHash();
                String capeHash = CapeCache.getCape(skinHash);
                String actualCapeHash = null;
                if (textures.cape() != null) {
                    actualCapeHash = textures.cape().getHash();
                }
                if (capeHash != null && !capeHash.equals(actualCapeHash)) {
                    MinecraftProfileTexture capeTexture = new MinecraftProfileTexture(
                            "https://textures.minecraft.net/texture/" + capeHash,
                            Map.of()
                    );
                    MinecraftProfileTextures newTextures = new MinecraftProfileTextures(textures.skin(), capeTexture, textures.elytra(), textures.signatureState());
                    MoreCapes.LOGGER.info("Fetching textures again to apply the cape for {}", uuid);
                    return fetchSkinTextures(uuid, newTextures);
                }
            }
            return CompletableFuture.completedFuture(skinTextures);
        });
    }
}