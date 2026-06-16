package cn.geoair.map.tile.forge.core.support;


import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;

import cn.geoair.map.tile.forge.core.support.local.*;
import cn.geoair.map.tile.forge.core.support.s3.*;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;


import cn.hutool.extra.spring.SpringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * TileStorageSupport适配器
 * 根据图层配置（LayerZipConfigPo）动态获取对应的TileStorageSupport实例
 */
//@Component
@RequiredArgsConstructor
public class TileStorageSupportAdapter {

    static TileStorageSupportAdapter instance;

    public static TileStorageSupportAdapter getInstance() {
        return instance == null ? instance = SpringUtil.getBean(TileStorageSupportAdapter.class) : instance;
    }

    /**
     * 缓存StorageType+TileFormat与TileStorageSupport实例的映射关系
     */
    private final Map<String, ITileStorageSupport> supportCache = new HashMap<>();

    /**
     * 根据图层名称获取对应的TileStorageSupport实例
     *
     * @param config 图层配置（包含StorageType和TileFormat）
     * @return 对应的TileStorageSupport实例
     */
    public ITileStorageSupport getSupport(GirLayerConfigContext config) {
        String layerName = config.getLayerName();
        // 1. 校验配置
        GirStorageType girStorageType = Optional.ofNullable(config.getStorageType())
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]未配置存储类型"));
        GirMapTileType mapTileType = Optional.ofNullable(config.getMapTileType())
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]未配置瓦片格式"));

        // 2. 生成缓存key（StorageType+TileFormat的唯一标识）
        String cacheKey = generateCacheKey(girStorageType, mapTileType);

        // 3. 从缓存获取实例，不存在则创建并缓存
        return supportCache.computeIfAbsent(cacheKey, k ->createSupportInstance(girStorageType, mapTileType));
    }

    /**
     * 生成缓存key（格式："存储类型_瓦片格式"）
     */
    private String generateCacheKey(GirStorageType girStorageType, GirMapTileType mapTileType) {
        return girStorageType.getValue() + "_" + mapTileType.getValue();
    }

    /**
     * 根据StorageType和TileFormat创建对应的TileStorageSupport实例
     */
    private ITileStorageSupport createSupportInstance(GirStorageType girStorageType, GirMapTileType mapTileType) {
        // 组合存储类型和瓦片格式，返回对应的实现类实例
        switch (girStorageType) {
            case LOCAL_ZIP:
                return createLocalZipSupport(mapTileType);
            case S3_ZIP:
                return createS3ZipSupport(mapTileType);
            case LOCAL_UNZIPPED:
                return createLocalUnzippedSupport(mapTileType);
            case S3_UNZIPPED:
                return createS3UnzippedSupport(mapTileType);
            default:
                throw new RuntimeException("不支持的存储类型：" + girStorageType.getValue());
        }
    }

    /**
     * 创建LOCAL_ZIP类型对应的实例
     */
    private ITileStorageSupport createLocalZipSupport(GirMapTileType mapTileType) {
        switch (mapTileType) {
            case COMPACT_V1:
                return new LocalZipCompactV1TileStorageSupport();
            case COMPACT_V2:
                return new LocalZipCompactV2TileStorageSupport();
            case LOOSE:
                return new LocalZipLooseTileStorageSupport();
            case TILE_3D:
                return new LocalZip3DTileStorageSupport();
            case TERRAIN_3D:
                return new LocalZip3DTerrainStorageSupport();
            case XYZ:
                return new LocalZipXYZTileStorageSupport();
            default:
                throw new RuntimeException("LOCAL_ZIP不支持的瓦片格式：" + mapTileType.getValue());
        }
    }

    /**
     * 创建S3_ZIP类型对应的实例
     */
    private ITileStorageSupport createS3ZipSupport(GirMapTileType mapTileType) {
        switch (mapTileType) {
            case COMPACT_V1:
                return new S3ZipCompactV1TileStorageSupport();
            case COMPACT_V2:
                return new S3ZipCompactV2TileStorageSupport();
            case XYZ:
                return new S3ZipXYZTileStorageSupport();
            case TILE_3D:
                return new S3Zip3DTileStorageSupport();
            case TERRAIN_3D:
                return new S3Zip3DTerrainStorageSupport();


            default:
                throw new RuntimeException("S3_ZIP不支持的瓦片格式：" + mapTileType.getValue());
        }
    }

    /**
     * 创建LOCAL_UNZIPPED类型对应的实例
     */
    private ITileStorageSupport createLocalUnzippedSupport(GirMapTileType mapTileType) {
        switch (mapTileType) {
            case COMPACT_V1:
                return new LocalUnzippedCompactV1TileStorageSupport();
            case COMPACT_V2:
                return new LocalUnzippedCompactV2TileStorageSupport();
            case XYZ:
                return new LocalUnzippedXYZTileStorageSupport();
//            case LOOSE:
//                return new LocalUnzippedLooseTileStorageSupport();
            default:
                throw new RuntimeException("LOCAL_UNZIPPED不支持的瓦片格式：" + mapTileType.getValue());
        }
    }

    /**
     * 创建S3_UNZIPPED类型对应的实例
     */
    private AbstractTileStorageSupport createS3UnzippedSupport(GirMapTileType mapTileType) {
        switch (mapTileType) {
            case COMPACT_V1:
                return new S3UnzippedCompactV1TileStorageSupport();
            case COMPACT_V2:
                return new S3UnzippedCompactV2TileStorageSupport();
            case XYZ:
                return new S3UnzippedXYZTileStorageSupport();
//            case LOOSE:
//                return new S3UnzippedLooseTileStorageSupport();
            default:
                throw new RuntimeException("S3_UNZIPPED不支持的瓦片格式：" + mapTileType.getValue());
        }
    }
}
