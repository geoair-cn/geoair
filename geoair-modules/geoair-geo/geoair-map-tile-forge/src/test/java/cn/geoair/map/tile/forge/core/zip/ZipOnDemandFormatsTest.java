package cn.geoair.map.tile.forge.core.zip;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.support.TileStorageSupportAdapter;
import cn.geoair.map.tile.forge.core.support.arcgis.ArcgisConfigXmlGetter;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** 从应用迁移，使用原生适配器验证全部15种 ZIP 组合，S3 仅模拟有范围的读取。 */
@RunWith(Parameterized.class)
public class ZipOnDemandFormatsTest {
    private static final byte[] TILE = {(byte) 137, 80, 78, 71, 1, 2, 3, 4};
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final GirStorageType storage;
    private final GirMapTileType type;
    private String previous;
    private S3ClientGetter previousS3;
    private final AtomicInteger ranges = new AtomicInteger();

    public ZipOnDemandFormatsTest(GirStorageType storage, GirMapTileType type) {
        this.storage = storage;
        this.type = type;
    }

    @Parameterized.Parameters(name = "{0}/{1}")
    public static Collection<Object[]> combinations() {
        Collection<Object[]> values = new ArrayList<>();
        for (GirStorageType storage :
                new GirStorageType[] {GirStorageType.LOCAL_ZIP, GirStorageType.S3_ZIP})
            for (GirMapTileType type : GirMapTileType.values())
                if (storage != GirStorageType.S3_ZIP || type != GirMapTileType.LOOSE)
                    values.add(new Object[] {storage, type});
        return values;
    }

    @Before
    public void setup() {
        previous = TileTempPathConfig.getInstance().getTempPath();
        previousS3 = S3CompressionHandler.s3ClientGetter;
        TileTempPathConfig.getInstance().setTempPath(folder.getRoot().toString());
    }

    @After
    public void restore() {
        TileTempPathConfig.getInstance().setTempPath(previous);
        S3CompressionHandler.s3ClientGetter = previousS3;
    }

    @Test
    public void readsBeforeIndexAndAfterIndexWithoutSwappingColumnsAndRows() throws Exception {
        Path source = folder.getRoot().toPath().resolve("中文 数据.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(source))) {
            for (Map.Entry<String, byte[]> entry : fixture(type).entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        GirLayerConfigContext context =
                new GirLayerConfigContext()
                        .setDataId("fixture_" + type)
                        .setStorageType(storage)
                        .setMapTileType(type)
                        .setObjectKey(source.toString())
                        .setFormat(
                                type == GirMapTileType.MVT_TILES
                                        ? "pbf"
                                        : type == GirMapTileType.TERRAIN_3D ? "terrain" : "png");
        if (storage == GirStorageType.S3_ZIP) {
            simulateS3(Files.readAllBytes(source));
            context.setObjectKey("source/data.zip");
        }
        ITileStorageSupport support =
                new TileStorageSupportAdapter(new ZipFixtureHelper()).getSupport(context);
        assertTile(support, context);
        if (support instanceof ArcgisConfigXmlGetter) {
            assertEquals("<CacheInfo/>", ((ArcgisConfigXmlGetter) support).getConfigXml(context));
            assertEquals("<Envelope/>", ((ArcgisConfigXmlGetter) support).getConfigCdi(context));
        }
        String metadata =
                type == GirMapTileType.TILE_3D
                        ? "tileset.json"
                        : type == GirMapTileType.S3M
                                ? "tilesetS3MB.scp"
                                : type == GirMapTileType.MVT_TILES
                                        ? "style.json"
                                        : type == GirMapTileType.TERRAIN_3D ? "layer.json" : null;
        if (metadata != null) assertTrue(support.getTileData(context, metadata, "", "").isExists());
        ((ZipDirectoryGetter) support).preCacheCentralDir(context);
        support = new TileStorageSupportAdapter(new ZipFixtureHelper()).getSupport(context);
        // 清掉本地正文后复核索引路径；V1 特意保留 bundlx，必须补齐 bundle。
        Path extracted = folder.getRoot().toPath().resolve("zip_resources");
        try (Stream<Path> files = Files.walk(extracted)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator)
                if (!file.toString().endsWith(".bundlx")) Files.delete(file);
        }
        assertTile(support, context);
        if (storage == GirStorageType.S3_ZIP) {
            assertTrue(ranges.get() > 0);
            try (Stream<Path> files = Files.walk(extracted)) {
                assertFalse(files.anyMatch(p -> p.toString().endsWith(".zip")));
            }
        }
    }

    private void assertTile(ITileStorageSupport support, GirLayerConfigContext context)
            throws Exception {
        TileRequest response =
                type == GirMapTileType.TILE_3D
                        ? support.getTileData(context, "parts/model.b3dm", "", "")
                        : type == GirMapTileType.S3M
                                ? support.getTileData(context, "parts/model.s3mb", "", "")
                                : type == GirMapTileType.MVT_TILES
                                        ? support.getTileData(context, "4/2/3.pbf", "", "")
                                        : support.getTileData(context, "4", "2", "3");
        assertTrue(type.name(), response.isExists());
        assertArrayEquals(TILE, response.getBytes());
        if (type == GirMapTileType.XYZ)
            assertTrue(support.getTileData(context, "0", "0", "0").isExists());
    }

    private void simulateS3(byte[] archive) {
        AmazonS3 client =
                (AmazonS3)
                        Proxy.newProxyInstance(
                                AmazonS3.class.getClassLoader(),
                                new Class<?>[] {AmazonS3.class},
                                (proxy, method, args) -> {
                                    ObjectMetadata metadata = new ObjectMetadata();
                                    metadata.setContentLength(archive.length);
                                    metadata.setHeader("ETag", "version-1");
                                    if (method.getName().equals("getObjectMetadata"))
                                        return metadata;
                                    if (method.getName().equals("getObject")) {
                                        GetObjectRequest request = (GetObjectRequest) args[0];
                                        assertEquals("bucket", request.getBucketName());
                                        assertNotNull("不能下载整个归档", request.getRange());
                                        long[] range = request.getRange();
                                        ranges.incrementAndGet();
                                        S3Object object = new S3Object();
                                        object.setObjectMetadata(metadata);
                                        object.setObjectContent(
                                                new ByteArrayInputStream(
                                                        Arrays.copyOfRange(
                                                                archive,
                                                                (int) range[0],
                                                                (int) range[1] + 1)));
                                        return object;
                                    }
                                    throw new UnsupportedOperationException(method.getName());
                                });
        S3CompressionHandler.s3ClientGetter = new ZipTestS3ClientGetter(client);
    }

    static Map<String, byte[]> fixture(GirMapTileType type) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        String base = "外层/数据/";
        if (type == GirMapTileType.COMPACT_V1
                || type == GirMapTileType.COMPACT_V2
                || type == GirMapTileType.LOOSE) {
            files.put(base + "conf.xml", "<CacheInfo/>".getBytes(StandardCharsets.UTF_8));
            files.put(base + "conf.cdi", "<Envelope/>".getBytes(StandardCharsets.UTF_8));
        }
        switch (type) {
            case XYZ:
                files.put(base + "0/0/0.png", TILE);
                files.put(base + "4/2/3.png", TILE);
                break;
            case MVT_TILES:
                files.put(base + "style.json", new byte[] {123, 125});
                files.put(base + "4/2/3.pbf", TILE);
                break;
            case TILE_3D:
                files.put(base + "tileset.json", new byte[] {123, 125});
                files.put(base + "parts/model.b3dm", TILE);
                break;
            case S3M:
                files.put(base + "root.scp", new byte[] {123, 125});
                files.put(base + "parts/model.s3mb", TILE);
                break;
            case TERRAIN_3D:
                files.put(base + "layer.json", new byte[] {123, 125});
                files.put(base + "4/2/3.terrain", TILE);
                break;
            case LOOSE:
                files.put(base + "_alllayers/L04/R00000003/C00000002.png", TILE);
                break;
            case COMPACT_V1:
                byte[] index = new byte[16 + 128 * 128 * 5 + 16];
                index[16 + (2 * 128 + 3) * 5] = 64;
                byte[] bundle = new byte[68 + TILE.length];
                ByteBuffer.wrap(bundle).order(ByteOrder.LITTLE_ENDIAN).putInt(64, TILE.length);
                System.arraycopy(TILE, 0, bundle, 68, TILE.length);
                files.put(base + "_alllayers/L04/R0000C0000.bundlx", index);
                files.put(base + "_alllayers/L04/R0000C0000.bundle", bundle);
                break;
            case COMPACT_V2:
                int offset = 64 + 128 * 128 * 8 + 4;
                byte[] v2 = new byte[offset + TILE.length];
                ByteBuffer.wrap(v2)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putLong(64 + (3 * 128 + 2) * 8, ((long) TILE.length << 40) | offset)
                        .putInt(offset - 4, TILE.length);
                System.arraycopy(TILE, 0, v2, offset, TILE.length);
                files.put(base + "_alllayers/L04/R0000C0000.bundle", v2);
                break;
            default:
                throw new IllegalArgumentException(type.name());
        }
        return files;
    }
}
