package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCache;
import cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCacheV1;
import cn.geoair.map.tile.forge.core.bygwc.compact.BundleFileResource;

import cn.geoair.map.tile.forge.core.support.arcgis.ConfigXmlGetterLocal;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.hutool.core.io.IoUtil;
import org.springframework.http.MediaType;

/**
 * 本地解压版Compact V1瓦片存储支持类
 *
 * @author 张俊
 * &#064;date Created in 2025/11/13 17:55
 * &#064;description 提供对本地解压后的Compact V1格式瓦片数据的访问支持
 */
public class LocalUnzippedCompactV1TileStorageSupport extends ConfigXmlGetterLocal {

    /**
     * 根据图层名称和瓦片坐标获取瓦片数据
     *
     * @param layerConfigContext 图层名称
     * @param z                  瓦片级别
     * @param x                  瓦片列号
     * @param y                  瓦片行号
     * @return TileRequest 瓦片请求对象
     * @throws Exception 获取瓦片数据时可能抛出的异常
     */
    @Override
    public TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception {
        TileRequest tileRequest = TileRequest.emptyByContext(layerConfigContext);
        String rootPath = getRootPath(layerConfigContext);
        ArcGISCompactCache gisCompactCache = getArcGISCompactCache(rootPath);
        BundleFileResource bundleFileResource = gisCompactCache.getBundleFileResource(Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));
        String format = layerConfigContext.getFormat();
        if (format == null) {
            format = "png";
        }
        if (bundleFileResource != null) {
            long size = bundleFileResource.getSize();
            tileRequest.setExists(true);
            tileRequest.setBytes(IoUtil.readBytes(bundleFileResource.getInputStream()));
            tileRequest.setSize(size);
            tileRequest.setLastModified(bundleFileResource.getLastModified());
            tileRequest.mimeTypeByType(MediaType.parseMediaType("image/" + format));
        }
        return tileRequest;
    }

    ArcGISCompactCache getArcGISCompactCache(String pathToCacheRoot) {
        return new ArcGISCompactCacheV1(pathToCacheRoot);
    }


    private String getRootPath(GirLayerConfigContext layerConfigContext) {
        return layerConfigContext.getObjectKey();

    }


}
