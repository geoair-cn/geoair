package cn.geoair.map.tile.forge.core.service;

import cn.geoair.base.bean.GirBeanHelper;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.cache.TileCacheRegistry;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.support.TileStorageSupportAdapter;
import cn.geoair.map.tile.forge.core.support.arcgis.ArcgisConfigXmlGetter;
import cn.geoair.map.tile.forge.core.utils.ForgeExecutorUtils;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.springframework.http.MediaType;

public class GirMapTileService {
    public static GiLogger log = GirLoggerFactory.getLogger();
    static GirMapTileService self = null;
    private static final Set<String> PRECACHING_LAYERS = ConcurrentHashMap.newKeySet();
    @Getter GirLayerConfigContextHelper contextHelper;
    @Getter TileStorageSupportAdapter tileStorageSupportAdapter;

    public GirMapTileService(
            GirLayerConfigContextHelper contextHelper,
            TileStorageSupportAdapter tileStorageSupportAdapter) {
        this.contextHelper = contextHelper;
        this.tileStorageSupportAdapter = tileStorageSupportAdapter;
        self = this;
    }

    public static GirMapTileService getInstance() {
        if (self == null) {
            self = GirBeanHelper.getProvider().getBean(GirMapTileService.class);
        }
        return self;
    }

    /**
     * 获取图层的瓦片数据
     *
     * @param layerName 图层名称
     * @return 瓦片数据
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public TileRequest getLayerTile(String layerName, String z, String y, String x)
            throws Exception {
        // 1. 查询图层配置
        GirLayerConfigContext config =
                contextHelper
                        .getByLayerName(layerName)
                        .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));
        return getLayerTile(config, z, y, x);
    }

    public TileRequest getLayerTile(GirLayerConfigContext config, String z, String y, String x)
            throws Exception {
        // 2. 通过适配器获取对应的TileStorageSupport实例
        ITileStorageSupport storageSupport = tileStorageSupportAdapter.getSupport(config);

        // 3. 调用实例方法获取瓦片数据
        return storageSupport.getTileData(config, z, x, y);
    }

    /** 获取图层的瓦片数据 */
    public TileRequest getCapabilities(String layerName) throws Exception {
        // 1. 查询图层配置
        GirLayerConfigContext config =
                contextHelper
                        .getByLayerName(layerName)
                        .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));

        // 2. 通过适配器获取对应的TileStorageSupport实例
        ITileStorageSupport storageSupport = tileStorageSupportAdapter.getSupport(config);
        TileRequest tileRequest = new TileRequest();
        if (storageSupport instanceof ArcgisConfigXmlGetter) {
            ArcgisConfigXmlGetter arcgisConfigXmlGetter = (ArcgisConfigXmlGetter) storageSupport;
            // 3. 调用实例方法获取瓦片数据
            String configXml = arcgisConfigXmlGetter.getCapabilities(config);
            if (configXml != null) {
                byte[] bytes = configXml.getBytes(StandardCharsets.UTF_8);
                tileRequest.setBytes(bytes);
                tileRequest.mimeTypeBySpring(MediaType.APPLICATION_XML);
                tileRequest.setExists(true);
                tileRequest.setSize(bytes.length);
                tileRequest.setLastModified(System.currentTimeMillis());
                tileRequest.setLayerName(layerName);
                tileRequest.setMapTileType(config.getMapTileType());
                tileRequest.setStorageType(config.getStorageType());
                return tileRequest;
            }
        }
        tileRequest.setBytes("无法找到配置文件".getBytes(StandardCharsets.UTF_8));
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

        GirLayerConfigContext config =
                contextHelper
                        .getByLayerName(layerName)
                        .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));

        ITileStorageSupport storageSupport = tileStorageSupportAdapter.getSupport(config);
        if (!PRECACHING_LAYERS.add(layerName)) {
            log.warn("图层正在预缓存，忽略重复请求：{}", layerName);
            return;
        }
        log.info("开始预缓存图层：{}, 执行器 {}", layerName, storageSupport.getClass().getName());
        // 使用共享且有界的执行器，避免每次请求都创建一个原生线程。
        try {
            ForgeExecutorUtils.getExecutor()
                    .execute(
                            () -> {
                                try {
                                    log.info("异步线程开始预缓存图层：{}", layerName);
                                    storageSupport.preCacheTiles(
                                            config, TileCacheRegistry.getDefaultTileCache());
                                    log.info("异步线程预缓存图层完成：{}", layerName);
                                } catch (Exception e) {
                                    log.error("图层预缓存失败：{}", layerName, e);
                                } finally {
                                    PRECACHING_LAYERS.remove(layerName);
                                }
                            });
        } catch (RuntimeException e) {
            PRECACHING_LAYERS.remove(layerName);
            throw e;
        }
    }
}
