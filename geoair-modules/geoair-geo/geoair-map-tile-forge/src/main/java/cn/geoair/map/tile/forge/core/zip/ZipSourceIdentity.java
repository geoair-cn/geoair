package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 只读取源文件元信息，不对数百GB正文计算哈希。配置和源版本共同隔离索引及提取文件。 */
public final class ZipSourceIdentity {
    private ZipSourceIdentity() {}

    public static String of(GirLayerConfigContext context, ICompressionHandler handler) {
        String identity =
                context.getLayerName()
                        + "\0"
                        + context.getDataId()
                        + "\0"
                        + context.getStorageType()
                        + "\0"
                        + context.getMapTileType()
                        + "\0"
                        + context.getObjectKey()
                        + "\0"
                        + context.getTilePathPrefix()
                        + "\0"
                        + context.getFormat()
                        + "\0"
                        + handler.getSourceVersion(context.getObjectKey());
        try {
            byte[] bytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
