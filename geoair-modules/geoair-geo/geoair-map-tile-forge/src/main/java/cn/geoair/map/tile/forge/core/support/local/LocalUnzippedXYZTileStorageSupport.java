package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.hutool.core.io.FileUtil;
import org.springframework.http.MediaType;

import java.io.File;

/**
 * 本地解压XYZ瓦片存储支持类
 * 用于处理ArcGIS瓦片数据的读取操作
 *
 * @author 张俊
 * @since 2025/11/17
 */
public class LocalUnzippedXYZTileStorageSupport implements ITileStorageSupport {

    /**
     * 根据图层配置和瓦片坐标获取瓦片数据
     * 检查本地是否存在对应瓦片文件，如果存在则直接读取
     *
     * @param layerConfigContext 图层配置信息对象，包含瓦片存储路径等配置信息
     * @param z              瓦片级别(Zoom Level)
     * @param x              瓦片列号(X Coordinate)
     * @param y              瓦片行号(Y Coordinate)
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

        String inLocalPath = getTilePath(layerConfigContext.getObjectKey(), z, y, x, format, File.separator);

        File localTileFile = new File(inLocalPath);
        if (!localTileFile.exists()) {
            return tileRequest;
        }
        tileRequest.setBytes(FileUtil.readBytes(localTileFile));
        tileRequest.setLastModified(localTileFile.lastModified());
        tileRequest.setSize(localTileFile.length());
        tileRequest.setExists(true);
        tileRequest.mimeTypeByType(MediaType.parseMediaType("image/" + format));
        return tileRequest;
    }

    @Override
    public void preCacheTiles(GirLayerConfigContext layerConfigContext, TileCache tileCache, ProgressConsumer progressConsumer) {

    }

    /**
     * 构建瓦片文件的完整路径
     * 根据图层配置、瓦片坐标和格式信息拼接成本地文件路径
     *
     * @param baseDir   基础文件路径
     * @param z         瓦片级别(Zoom Level)
     * @param y         瓦片行号(Y Coordinate)
     * @param x         瓦片列号(X Coordinate)
     * @param format    瓦片图像格式，如 png、jpg 等
     * @param separator 路径分隔符，通常为系统默认分隔符
     * @return String 完整的瓦片文件路径字符串
     */
    protected String getTilePath(String baseDir, String z, String y, String x, String format, String separator) {
        StringBuilder pathBuilder = new StringBuilder();

        String objectKey = baseDir;
        if (objectKey.endsWith(separator)) {
            objectKey = objectKey.substring(0, objectKey.length() - 1);
        }
        pathBuilder.append(objectKey).append(separator)
                .append(z).append(separator)
                .append(x).append(separator)
                .append(y).append(".").append(format);
        return pathBuilder.toString().trim();
    }




}
