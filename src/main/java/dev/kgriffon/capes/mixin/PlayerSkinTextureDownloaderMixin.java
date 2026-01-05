package dev.kgriffon.capes.mixin;

import dev.kgriffon.capes.MoreCapes;
import dev.kgriffon.capes.util.CapeCache;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(PlayerSkinTextureDownloader.class)
public class PlayerSkinTextureDownloaderMixin {

    @Inject(at = @At("RETURN"), method = "download")
    private static void download(Path path, String url, CallbackInfoReturnable<NativeImage> cir) {

        String[] splitUrl = url.split("/");
        String skinHash = splitUrl[splitUrl.length - 1];

        if (!CapeCache.contains(skinHash)) {
            NativeImage image = cir.getReturnValue();

            if (String.format("%08X", image.getColorArgb(0, 0)).equalsIgnoreCase("CAFEBABE")) {
                StringBuilder capeHash = new StringBuilder();
                for (int i = 0; i < 8; i++) {
                    capeHash.append(String.format("%08X", image.getColorArgb(i, 1)));
                }
                String finalHash = capeHash.toString().replaceFirst("^0+", "");
                MoreCapes.LOGGER.info("Cape {} found for the skin {}", finalHash.toLowerCase(), skinHash);
                CapeCache.registerCape(skinHash, finalHash.toLowerCase());
            } else {
                CapeCache.registerCape(skinHash, null);
            }
        }
    }

}
