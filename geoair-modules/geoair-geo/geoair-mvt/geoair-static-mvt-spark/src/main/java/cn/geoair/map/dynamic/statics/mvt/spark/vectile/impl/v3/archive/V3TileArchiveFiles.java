package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.archive;

import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * V3 单文件瓦片归档的本地目录扫描工具。
 *
 * @author 张逢吉
 */
final class V3TileArchiveFiles {

    private V3TileArchiveFiles() {
    }

    /** 扫描 {@code z/x/y.pbf} 目录，并统一换算为 XYZ 行号。 */
    static List<TileFile> listTiles(Path stagingDirectory, TileYAxis stagingYAxis) throws IOException {
        if (stagingDirectory == null || !Files.isDirectory(stagingDirectory)) {
            throw new IllegalArgumentException("V3 归档中间目录不存在: " + stagingDirectory);
        }
        List<TileFile> tiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(stagingDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".pbf"))
                    .forEach(path -> tiles.add(parseTile(stagingDirectory, path, stagingYAxis)));
        }
        if (tiles.isEmpty()) {
            throw new IllegalArgumentException("V3 归档中间目录不包含任何 .pbf 瓦片: " + stagingDirectory);
        }
        return tiles;
    }

    /** 将 TMS 行号转换为 XYZ 行号；XYZ 输入直接返回。 */
    static int toXyzY(int z, int y, TileYAxis sourceYAxis) {
        return sourceYAxis == TileYAxis.TMS ? maxY(z) - y : y;
    }

    /** 将 XYZ 行号转换为 TMS 行号。 */
    static int toTmsY(int z, int y) {
        return maxY(z) - y;
    }

    /** 按 PMTiles TileID 的升序排列。 */
    static void sortByPmtilesId(List<TileFile> tiles) {
        Collections.sort(tiles, Comparator.comparingLong(tile -> PmtilesUtils.tileId(tile.z, tile.x, tile.xyzY)));
    }

    private static TileFile parseTile(Path root, Path path, TileYAxis sourceYAxis) {
        Path relative = root.relativize(path);
        if (relative.getNameCount() != 3) {
            throw new IllegalArgumentException("V3 瓦片必须采用 z/x/y.pbf 目录结构: " + path);
        }
        String zText = relative.getName(0).toString();
        String xText = relative.getName(1).toString();
        String fileName = relative.getName(2).toString();
        String yText = fileName.substring(0, fileName.length() - ".pbf".length());
        try {
            int z = Integer.parseInt(zText);
            int x = Integer.parseInt(xText);
            int sourceY = Integer.parseInt(yText);
            validateCoordinate(z, x, sourceY, path);
            int xyzY = toXyzY(z, sourceY, sourceYAxis);
            return new TileFile(z, x, sourceY, xyzY, path, Files.size(path));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("V3 瓦片路径必须使用整数 z/x/y: " + path, e);
        } catch (IOException e) {
            throw new IllegalStateException("读取 V3 瓦片文件大小失败: " + path, e);
        }
    }

    private static void validateCoordinate(int z, int x, int y, Path path) {
        if (z < 0 || z > 29) {
            throw new IllegalArgumentException("PMTiles/MBTiles 归档仅支持 0-29 级: " + path);
        }
        int max = maxY(z);
        if (x < 0 || x > max || y < 0 || y > max) {
            throw new IllegalArgumentException("V3 瓦片坐标超出级别范围: " + path);
        }
    }

    private static int maxY(int z) {
        return (1 << z) - 1;
    }

    /** 本地瓦片文件及其行号转换信息。 */
    static final class TileFile {
        final int z;
        final int x;
        final int sourceY;
        final int xyzY;
        final Path path;
        final long length;

        TileFile(int z, int x, int sourceY, int xyzY, Path path, long length) {
            this.z = z;
            this.x = x;
            this.sourceY = sourceY;
            this.xyzY = xyzY;
            this.path = path;
            this.length = length;
        }
    }
}
