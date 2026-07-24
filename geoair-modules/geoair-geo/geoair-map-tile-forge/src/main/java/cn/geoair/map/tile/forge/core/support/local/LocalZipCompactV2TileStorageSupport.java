package cn.geoair.map.tile.forge.core.support.local;

import static cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCache.BUNDLE_EXT;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCache;
import cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCacheV2;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import java.io.File;
import java.io.IOException;

/**
 * @author ：张俊 &#064;date ：Created in 2025/11/17 09:49
 *     &#064;description：本地ZIP压缩V2版本瓦片存储支持类，用于处理ArcGIS Compact Cache V2格式的瓦片数据
 *     支持从ZIP压缩包中解压.bundle文件并读取特定行列层级的瓦片数据
 */
public class LocalZipCompactV2TileStorageSupport extends LocalZipCompactV1TileStorageSupport {
    public LocalZipCompactV2TileStorageSupport(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
    }

    ArcGISCompactCache getArcGISCompactCache(String pathToCacheRoot) {
        return new ArcGISCompactCacheV2(pathToCacheRoot);
    }

    protected void byZip(
            GirLayerConfigContext layerConfigContext,
            String z,
            String y,
            String x,
            String tempDirAbsolutePath)
            throws IOException {
        ArcGISCompactCache arcGISCompactCache =
                getArcGISCompactCache(layerConfigContext.getTilePathPrefix());
        String filePath =
                arcGISCompactCache.buildBundleFilePath(
                        Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));
        zipBundleFileToLocal(layerConfigContext, filePath, tempDirAbsolutePath, BUNDLE_EXT);
    }

    protected void byPreCache(
            GirLayerConfigContext layerConfigContext,
            String z,
            String y,
            String x,
            String tempDirAbsolutePath)
            throws IOException {
        ArcGISCompactCache arcGISCompactCache = getArcGISCompactCache(File.separator);
        String filePath =
                arcGISCompactCache.buildBundleFilePath(
                        Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));
        boolean b =
                preCacheBundleFileToLocal(
                        layerConfigContext, filePath, tempDirAbsolutePath, BUNDLE_EXT);
        if (!b) {
            return;
        }
    }

    @Override
    public TileCentralDirectoryModel tranToTileModel(CentralDirectoryModel centralDirectoryModel) {
        return super.tranToTileModel(centralDirectoryModel);
    }
}
