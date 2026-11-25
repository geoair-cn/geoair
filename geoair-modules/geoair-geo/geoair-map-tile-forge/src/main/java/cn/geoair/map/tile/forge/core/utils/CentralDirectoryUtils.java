package cn.geoair.map.tile.forge.core.utils;

import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import java.sql.SQLException;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/2 11:30
 * @description： TODO
 */
public class CentralDirectoryUtils {

    public static void doInsert(
            List<TileCentralDirectoryModel> batchList, LayerPerFileDao layerPerFileDao) {
        // 扫描流水线已经隔离生产与消费；此处等待事务提交，向扫描端施加背压。
        try {
            layerPerFileDao.batchInsert(batchList);
        } catch (SQLException e) {
            throw new IllegalStateException("归档索引批次写入失败", e);
        }
    }
}
