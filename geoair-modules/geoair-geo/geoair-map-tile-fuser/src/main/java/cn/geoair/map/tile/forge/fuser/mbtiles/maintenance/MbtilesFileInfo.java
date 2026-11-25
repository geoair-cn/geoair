package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import cn.geoair.base.data.model.annotation.GaModelField;
import lombok.Data;

/** 只读页信息即可返回，不逐条统计大库瓦片。 */
@Data
public class MbtilesFileInfo {
    @GaModelField(text = "物理文件路径")
    private String path;

    @GaModelField(text = "主文件占用字节")
    private long mainBytes;

    @GaModelField(text = "WAL占用字节")
    private long walBytes;

    @GaModelField(text = "SHM占用字节")
    private long shmBytes;

    @GaModelField(text = "数据库页大小")
    private long pageSize;

    @GaModelField(text = "数据库页数")
    private long pageCount;

    @GaModelField(text = "空闲页数")
    private long freePages;

    @GaModelField(text = "空闲页占用字节")
    private long freeBytes;

    @GaModelField(text = "磁盘可用字节")
    private long diskFreeBytes;

    @GaModelField(text = "数据库回收模式")
    private int autoVacuum;

    @GaModelField(text = "数据库日志模式")
    private String journalMode;

    @GaModelField(text = "支持增量回收")
    private boolean incrementalSupported;

    @GaModelField(text = "完整整理空间充足")
    private boolean vacuumSpaceEnough;
}
