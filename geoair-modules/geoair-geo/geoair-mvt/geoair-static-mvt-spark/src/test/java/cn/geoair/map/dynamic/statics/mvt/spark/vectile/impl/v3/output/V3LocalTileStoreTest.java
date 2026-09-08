package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.output;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputConfig;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** V3 本地目录瓦片存储的基本行为验证。 */
public class V3LocalTileStoreTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldWriteTileAndMetadataUsingZxyPath() throws Exception {
        File root = temporaryFolder.newFolder("v3-tiles");
        V3TileOutputConfig config = V3TileOutputConfig.localDirectory(root.getAbsolutePath());
        byte[] pbf = new byte[]{1, 2, 3, 4};
        byte[] metadata = "{\"format\":\"pbf\"}".getBytes(StandardCharsets.UTF_8);

        try (V3TileStore store = V3TileStoreFactory.open(config)) {
            store.writeTile(8, 216, 103, pbf, true);
            store.writeMetadata(metadata);
        }

        Assert.assertArrayEquals(pbf, Files.readAllBytes(root.toPath().resolve("8/216/103.pbf")));
        Assert.assertArrayEquals(metadata, Files.readAllBytes(root.toPath().resolve("geoair-v3-mvt.json")));
    }

    @Test
    public void shouldRejectOverwriteWhenDisabled() throws Exception {
        File root = temporaryFolder.newFolder("non-overwrite");
        V3TileOutputConfig config = V3TileOutputConfig.localDirectory(root.getAbsolutePath())
                .setOverwrite(false);
        try (V3TileStore store = V3TileStoreFactory.open(config)) {
            store.writeTile(1, 1, 1, new byte[]{1}, true);
            try {
                store.writeTile(1, 1, 1, new byte[]{2}, true);
                Assert.fail("关闭覆盖时应拒绝写入相同瓦片");
            } catch (IOException expected) {
                Assert.assertTrue(expected.getMessage().contains("已存在"));
            }
        }
    }
}
