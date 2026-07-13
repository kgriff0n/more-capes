package dev.kgriffon.capes.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import dev.kgriffon.capes.MoreCapes;
import dev.kgriffon.capes.util.CapeCache;
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
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.server.Services;
import net.minecraft.world.entity.player.PlayerSkin;

@Mixin(SkinManager.class)
public abstract class PlayerSkinProviderMixin {

    @Shadow
    protected abstract CompletableFuture<PlayerSkin> registerTextures(UUID profileId, MinecraftProfileTextures textures);

    @Inject(at = @At("HEAD"), method = "<init>")
    private static void getAssetsCache(Path skinsDirectory, Services services, SkinTextureDownloader skinTextureDownloader, Executor mainThreadExecutor, CallbackInfo ci) {
        CapeCache.setAssetsCache(skinsDirectory);
    }

    @ModifyReturnValue(
            at = @At("RETURN"),
            method = "registerTextures(Ljava/util/UUID;Lcom/mojang/authlib/minecraft/MinecraftProfileTextures;)Ljava/util/concurrent/CompletableFuture;"
    )
    private CompletableFuture<PlayerSkin> fetchTextures(CompletableFuture<PlayerSkin> original, UUID profileId, MinecraftProfileTextures textures) {

        return original.thenCompose(skinTextures -> {
            if (textures.skin() != null) {
                String skinHash = textures.skin().getHash();
                String capeHash = CapeCache.getCape(skinHash);
                String actualCapeHash = null;
                if (textures.cape() != null) {
                    actualCapeHash = textures.cape().getHash();
                }
                MoreCapes.LOGGER.info("Old cape {} | New cape {}", actualCapeHash, capeHash);
                if (capeHash != null && !capeHash.equals(actualCapeHash)) {
                    MinecraftProfileTexture capeTexture = new MinecraftProfileTexture(
                            "https://textures.minecraft.net/texture/" + capeHash,
                            Map.of()
                    );
                    MinecraftProfileTextures newTextures = new MinecraftProfileTextures(textures.skin(), capeTexture, textures.elytra(), textures.signatureState());
                    MoreCapes.LOGGER.info("Fetching textures again to apply the cape {} for {}", capeHash, profileId);
                    return registerTextures(profileId, newTextures);
                }
            }
            return CompletableFuture.completedFuture(skinTextures);
        });
    }
}