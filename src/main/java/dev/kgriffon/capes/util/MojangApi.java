package dev.kgriffon.capes.util;

import dev.kgriffon.capes.MoreCapes;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class MojangApi {

    private static final String API_URL = "https://api.minecraftservices.com/minecraft/profile/skins";


    public static @Nullable String uploadSkin(String accessToken, File skinFile, String variant) {

        MoreCapes.LOGGER.info("Upload skin to Mojang API...");
        HttpResponse<JsonNode> response = Unirest.post(API_URL)
                .header("Authorization", "Bearer " + accessToken)
                .field("variant", variant)
                .field("file", skinFile)
                .asJson();

        if (response.getStatus() != 200) {
            MoreCapes.LOGGER.info("Error {} while uploading cape: {}", response.getStatus(), response.getBody());
            return null;
        }

        return response.getBody().getObject().getJSONArray("skins").getJSONObject(0).getString("textureKey");
    }
}
