package dev.kgriffon.capes.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import dev.kgriffon.capes.MoreCapes;
import dev.kgriffon.capes.util.CapeCache;
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