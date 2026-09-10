package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.archive;

import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** V3 MBTiles、PMTiles 归档工具验证。 */
public class V3TileArchiveUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldArchiveDirectoryAsMbtilesUsingTmsRows() throws Exception {
        Path staging = temporaryFolder.newFolder("mbtiles-stage").toPath();
        writeTile(staging, 1, 0, 0, new byte[]{1, 2});
        Path archive = temporaryFolder.newFile("tiles.mbtiles").toPath();

        MbtilesArchiveUtils.archive(staging, archive, TileYAxis.XYZ, true, 100,
                MbtilesArchiveUtils.metadata("test", "v1", 1, 1, "{}"));

        Assert.assertTrue(MbtilesArchiveUtils.isMbtiles(archive));
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + archive.toAbsolutePath());
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT tile_row,tile_data FROM tiles WHERE zoom_level=1 AND tile_column=0");
             ResultSet result = statement.executeQuery()) {
            Assert.assertTrue(result.next());
            Assert.assertEquals("MBTiles 必须使用 TMS 行号", 1, result.getInt("tile_row"));
            Assert.assertArrayEquals(new byte[]{1, 2}, result.getBytes("tile_data"));
        }
    }

    @Test
    public void shouldCreatePmtilesV3HeaderAndTileIds() throws Exception {
        Path staging = temporaryFolder.newFolder("pmtiles-stage").toPath();
        writeTile(staging, 0, 0, 0, new byte[]{1});
        writeTile(staging, 1, 0, 0, new byte[]{2});
        writeTile(staging, 1, 0, 1, new byte[]{3});
        Path archive = temporaryFolder.newFile("tiles.pmtiles").toPath();

        PmtilesUtils.archive(staging, archive, TileYAxis.XYZ, true, true,
                "{\"vector_layers\":[],\"name\":\"test\"}");

        Assert.assertTrue(PmtilesUtils.isPmtilesV3(archive));
        Assert.assertEquals(0L, PmtilesUtils.tileId(0, 0, 0));
        Assert.assertEquals(1L, PmtilesUtils.tileId(1, 0, 0));
        Assert.assertEquals(2L, PmtilesUtils.tileId(1, 0, 1));
        PmtilesUtils.PmtilesHeader header = PmtilesUtils.readHeader(archive);
        Assert.assertEquals(PmtilesUtils.HEADER_LENGTH, header.getRootDirectoryOffset());
        Assert.assertEquals(3L, header.getAddressedTiles());
        Assert.assertEquals(3L, header.getTileContents());
    }

    private static void writeTile(Path staging, int z, int x, int y, byte[] data) throws Exception {
        Path path = staging.resolve(z + "/" + x + "/" + y + ".pbf");
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    }
}
