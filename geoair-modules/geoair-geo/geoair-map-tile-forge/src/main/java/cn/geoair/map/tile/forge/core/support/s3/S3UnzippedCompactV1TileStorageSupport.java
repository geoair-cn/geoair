package cn.geoair.map.tile.forge.core.support.s3;

import cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCacheV1;
import cn.geoair.map.tile.forge.core.bygwc.compact.BundleFileResource;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.support.ConfigXmlGetterS3;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

import static cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCache.BUNDLE_EXT;
import static cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCache.BUNDLX_EXT;

/**
 * @author ：张俊
 * &#064;date  ：Created in 2025/11/13 17:58
 * &#064;description：S3存储支持类，用于处理未压缩的紧凑型V1瓦片数据
 */
@Slf4j
public class S3UnzippedCompactV1TileStorageSupport extends ConfigXmlGetterS3 {


    @Override
    public TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception {
        TileRequest tileRequest = getTileRequest(layerConfigContext);

        // 构建远程文件路径
        ArcGISCompactCacheV1 remoteCache = new ArcGISCompactCacheV1(layerConfigContext.getTilePathPrefix());
        String remoteBasePath = remoteCache.buildBundleFilePath(Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));
        String remoteBundlxPath = remoteBasePath + BUNDLX_EXT;
        String remoteBundlePath = remoteBasePath + BUNDLE_EXT;

        // 构建本地临时目录路径
        // 构建本地临时目录路径
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);


        // 下载 .bundlx 和 .bundle 文件到本地临时目录
        S3ClientGetter.getInstance().downloadFromS3IfNeeded(layerConfigContext.getObjectKey(), remoteBundlxPath, tempDirAbsolutePath);
        S3ClientGetter.getInstance().downloadFromS3IfNeeded(layerConfigContext.getObjectKey(), remoteBundlePath, tempDirAbsolutePath);

        // 使用本地缓存访问器读取瓦片数据
        ArcGISCompactCacheV1 localCache = new ArcGISCompactCacheV1(tempDirAbsolutePath + File.separator + "_alllayers");
        BundleFileResource bundleFileResource = localCache.getBundleFileResource(Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));

        // 兼容中文版ArcGIS切片（路径为"图层"而非"_alllayers"）
        if (bundleFileResource == null) {
            localCache = new ArcGISCompactCacheV1(tempDirAbsolutePath + File.separator + "图层");
            bundleFileResource = localCache.getBundleFileResource(Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));
        }

        if (bundleFileResource != null) {
            long size = bundleFileResource.getSize();
            tileRequest.setExists(true);
            tileRequest.setBytes(IoUtil.readBytes(bundleFileResource.getInputStream()));
            tileRequest.setSize(size);
            tileRequest.setLastModified(bundleFileResource.getLastModified());
        }

        return tileRequest;
    }


}
