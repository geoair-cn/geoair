package cn.geoair.map.tile.forge.core.support.s3;

import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import cn.geoair.map.tile.forge.core.support.local.LocalUnzippedXYZTileStorageSupport;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.web.util.GutilMimeType;
import cn.hutool.core.io.FileUtil;

import java.io.File;

/**
 * S3解压XYZ瓦片存储支持类
 *
 * @author 张俊
 * @since 2025/11/17
 */
public class S3UnzippedXYZTileStorageSupport extends LocalUnzippedXYZTileStorageSupport {

    /**
     * 根据图层配置和瓦片坐标获取瓦片数据
     * 首先检查本地临时目录是否存在对应瓦片文件，如果不存在则从S3下载
     *
     * @param layerConfigContext 图层配置信息对象，包含瓦片存储路径等配置信息
     * @param z                  瓦片级别(Zoom Level)
     * @param x                  瓦片列号(X Coordinate)
     * @param y                  瓦片行号(Y Coordinate)
     * @return TileRequest 瓦片请求对象，包含瓦片数据流及相关元信息
     * @throws Exception 获取瓦片数据过程中可能出现的IO异常
     */
    @Override
    public TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception {
        TileRequest tileRequest = TileRequest.emptyByContext(layerConfigContext);

        String format = layerConfigContext.getFormat();
        if (format == null) {
            format = "png";
        }

        String remoteTilePath = getTilePath(layerConfigContext.getObjectKey(), z, y, x, format, "/");

        // 构建本地临时目录路径
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);

        String localTilePath = getTilePath(tempDirAbsolutePath, z, y, x, format, File.separator);

        File localTileFile = new File(localTilePath);
        if (!localTileFile.exists()) {
            S3ClientGetter.getInstance().downloadFromS3IfNeeded(layerConfigContext.getObjectKey(), remoteTilePath, localTilePath);
        }
        tileRequest.setBytes(FileUtil.readBytes(localTileFile));
        tileRequest.setLastModified(localTileFile.lastModified());
        tileRequest.setSize(localTileFile.length());
        tileRequest.setExists(true);
        tileRequest.setMimeType(GutilMimeType.fromExtension(format));
        return tileRequest;
    }


}
