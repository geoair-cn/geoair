package cn.geoair.map.tile.forge.core.zip;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.local.LocalZip3DTileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.zip.ZipFile;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 显式指定现场样本才运行；只读源ZIP，所有派生文件写入临时目录。 */
public class ZipExternalArchiveTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void readsCustomerArchiveBeforeIndexAndModelsBeyond4Gb() throws Exception {
        String path = System.getProperty("geoair.forge.test.archive");
        Assume.assumeTrue(path != null && !path.isEmpty());
        Path source = Paths.get(path);
        long size = Files.size(source);
        FileTime modified = Files.getLastModifiedTime(source);
        String previous = TileTempPathConfig.getInstance().getTempPath();
        TileTempPathConfig.getInstance().setTempPath(folder.getRoot().toString());
        try (ZipFile reference = new ZipFile(source.toFile())) {
            GirLayerConfigContext context =
                    new GirLayerConfigContext()
                            .setDataId("external")
                            .setStorageType(GirStorageType.LOCAL_ZIP)
                            .setMapTileType(GirMapTileType.TILE_3D)
                            .setObjectKey(path);
            LocalZip3DTileStorageSupport support =
                    new LocalZip3DTileStorageSupport(new ZipFixtureHelper());
            LocalCompressionHandler handler = new LocalCompressionHandler();
            RootPathInfo root = support.preCheckZipAndGetRoot(context, handler);
            assertArrayEquals(
                    read(reference.getInputStream(reference.getEntry(root.getRootFilePath()))),
                    support.getTileData(context, "tileset.json", "", "").getBytes());
            CentralDirectoryModel[] selected = {null};
            handler.scanAllEntries(
                    path,
                    (entry, all, current) -> {
                        if (!entry.isDirectoryIs()
                                && entry.getLocalHeaderOffset() > 0xffffffffL
                                && entry.getUncompressedSize() <= 2 * 1024 * 1024
                                && entry.getName().startsWith(root.getRootPath())) {
                            selected[0] = entry;
                            return false;
                        }
                        return true;
                    });
            assertNotNull("本项验收样本须含超过4GB偏移的文件", selected[0]);
            String name = selected[0].getName();
            assertArrayEquals(
                    read(reference.getInputStream(reference.getEntry(name))),
                    support.getTileData(
                                    context, name.substring(root.getRootPath().length()), "", "")
                            .getBytes());
            support.preCacheCentralDir(context);
            assertArrayEquals(
                    read(reference.getInputStream(reference.getEntry(name))),
                    new LocalZip3DTileStorageSupport(new ZipFixtureHelper())
                            .getTileData(
                                    context, name.substring(root.getRootPath().length()), "", "")
                            .getBytes());
        } finally {
            TileTempPathConfig.getInstance().setTempPath(previous);
        }
        assertEquals(size, Files.size(source));
        assertEquals(modified, Files.getLastModifiedTime(source));
    }

    private static byte[] read(InputStream input) throws Exception {
        try (InputStream stream = input;
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int size;
            while ((size = stream.read(buffer)) >= 0) output.write(buffer, 0, size);
            return output.toByteArray();
        }
    }
}
