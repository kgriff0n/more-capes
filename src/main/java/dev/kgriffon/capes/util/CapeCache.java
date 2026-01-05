package dev.kgriffon.capes.util;

import java.nio.file.Path;
import java.util.HashMap;

public class CapeCache {

    private static Path assetsCache = null;
    private static final HashMap<String, String> CAPES_HASH = new HashMap<>();

    public static void setAssetsCache(Path path) {
        assetsCache = path;
    }

    public static Path getAssetsCache() {
        return assetsCache;
    }

    public static void registerCape(String skinHash, String capeHash) {
        CAPES_HASH.put(skinHash, capeHash);
    }

    public static String getCape(String skinHash) {
        return CAPES_HASH.get(skinHash);
    }

    public static boolean contains(String skinHash) {
        return CAPES_HASH.containsKey(skinHash);
    }

}
