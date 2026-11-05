package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;

import lombok.Data;

/** 有效非空瓦片数量及数据字节数，不包含数据库页开销。 */
@Data
@GaModel(text = "缓存级别统计")
public class TileCacheLevelStatistics {
    @GaModelField(text = "级别")
    private int zoom;

    @GaModelField(text = "瓦片数量")
    private long tileCount;

    @GaModelField(text = "瓦片内容字节数")
    private long tileBytes;
}
