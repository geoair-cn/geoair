package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.output;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 将 V3 瓦片写入本地或共享文件系统的存储会话。
 *
 * @author 张逢吉
 */
final class V3LocalTileStore implements V3TileStore {

    private final Path rootDirectory;
    private final boolean overwrite;
    private final String metadataFileName;

    V3LocalTileStore(V3TileOutputConfig config) {
        this(config.getLocalDirectory(), config.isOverwrite(), config.getMetadataFileName());
    }

    V3LocalTileStore(String localDirectory, boolean overwrite, String metadataFileName) {
        this.rootDirectory = Paths.get(localDirectory).toAbsolutePath().normalize();
        this.overwrite = overwrite;
        this.metadataFileName = metadataFileName;
    }

    @Override
    public void writeTile(int z, int x, int y, byte[] data, boolean gzip) throws IOException {
        Path tilePath = rootDirectory.resolve(String.valueOf(z))
                .resolve(String.valueOf(x))
                .resolve(y + ".pbf").normalize();
        ensureWithinRoot(tilePath);
        Files.createDirectories(tilePath.getParent());
        write(tilePath, data);
    }

    @Override
    public void writeMetadata(byte[] data) throws IOException {
        Path metadataPath = rootDirectory.resolve(metadataFileName).normalize();
        ensureWithinRoot(metadataPath);
        Files.createDirectories(rootDirectory);
        write(metadataPath, data);
    }

    @Override
    public void close() {
        // 文件按单次写入关闭，无需额外释放资源。
    }

    private void write(Path path, byte[] data) throws IOException {
        if (!overwrite && Files.exists(path)) {
            throw new IOException("目标瓦片已存在且未开启覆盖: " + path);
        }
        Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private void ensureWithinRoot(Path path) {
        if (!path.startsWith(rootDirectory)) {
            throw new IllegalArgumentException("V3 本地输出路径不能越出根目录: " + path);
        }
    }
}
