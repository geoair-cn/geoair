package cn.geoair.map.tile.forge.core.service;

import cn.geoair.base.bean.GirBeanHelper;
import cn.geoair.map.tile.forge.core.cache.TileCacheRegistry;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.support.TileStorageSupportAdapter;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.support.arcgis.ArcgisConfigXmlGetter;
import cn.geoair.map.tile.forge.core.TileRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class GirMapTileService extends TileStorageSupportAdapter {

    static GirMapTileService self = null;

    public static GirMapTileService getInstance() {
        if (self == null) {
            self = GirBeanHelper.getProvider().getBean(GirMapTileService.class);
        }
        return self;
    }

    //    @Resource
//    private TileStorageSupportAdapter supportAdapter;
    @Resource
    GirLayerConfigContextHelper layerConfigHelper;

    /**
     * 获取图层的瓦片数据
     *
     * @param layerName 图层名称
     * @return 瓦片数据
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public TileRequest getLayerTile(String layerName, String z, String y, String x) throws Exception {
        // 1. 查询图层配置
        GirLayerConfigContext config = layerConfigHelper.getByLayerName(layerName)
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));
        return getLayerTile(config, z, y, x);
    }

    public TileRequest getLayerTile(GirLayerConfigContext config, String z, String y, String x) throws Exception {
        // 2. 通过适配器获取对应的TileStorageSupport实例
        ITileStorageSupport storageSupport = super.getSupport(config);

        // 3. 调用实例方法获取瓦片数据
        return storageSupport.getTileData(config, z, x, y);
    }

    /**
     * 获取图层的瓦片数据
     */
    public TileRequest getCapabilities(String layerName) throws Exception {
        // 1. 查询图层配置
        GirLayerConfigContext config = layerConfigHelper.getByLayerName(layerName)
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));

        // 2. 通过适配器获取对应的TileStorageSupport实例
        ITileStorageSupport storageSupport = super.getSupport(config);
        TileRequest tileRequest = new TileRequest();
        if (storageSupport instanceof ArcgisConfigXmlGetter) {
            ArcgisConfigXmlGetter arcgisConfigXmlGetter = (ArcgisConfigXmlGetter) storageSupport;
            // 3. 调用实例方法获取瓦片数据
            String configXml = arcgisConfigXmlGetter.getCapabilities(config);
            if (configXml != null) {
                tileRequest.setBytes(configXml.getBytes());
                tileRequest.mimeTypeBySpring(MediaType.APPLICATION_XML);
                tileRequest.setExists(true);
                tileRequest.setSize(configXml.getBytes().length);
                tileRequest.setLastModified(System.currentTimeMillis());
                tileRequest.setLayerName(layerName);
                tileRequest.setMapTileType(config.getMapTileType());
                tileRequest.setStorageType(config.getStorageType());
            }
        }
        tileRequest.setBytes(new String("无法找到配置文件").getBytes("UTF-8"));
        tileRequest.mimeTypeBySpring(MediaType.TEXT_XML);
        tileRequest.setExists(false);
        tileRequest.setSize(0);
        tileRequest.setLastModified(System.currentTimeMillis());
        tileRequest.setLayerName(layerName);
        tileRequest.setMapTileType(config.getMapTileType());
        tileRequest.setStorageType(config.getStorageType());
        return tileRequest;
    }


    public void preCacheTiles(String layerName) {

        GirLayerConfigContext config = layerConfigHelper.getByLayerName(layerName)
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));

        ITileStorageSupport storageSupport = super.getSupport(config);
        log.info("开始预缓存图层：{}, 执行器 {}", layerName, storageSupport.getClass().getName());
        // 创建新线程来执行预缓存任务
        Thread precacheThread = new Thread(() -> {
            try {
                log.info("异步线程开始预缓存图层：{}", layerName);
                storageSupport.preCacheTiles(config, TileCacheRegistry.getDefaultTileCache());
                log.info("异步线程预缓存图层完成：{}", layerName);
            } catch (Exception e) {
                // 记录异常日志
                e.printStackTrace();
            }
        });

        // 启动线程
        precacheThread.start();
    }

}
