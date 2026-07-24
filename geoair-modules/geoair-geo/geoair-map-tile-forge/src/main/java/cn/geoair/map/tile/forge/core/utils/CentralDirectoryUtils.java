package cn.geoair.map.tile.forge.core.utils;

import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/2 11:30
 * @description： TODO
 */
public class CentralDirectoryUtils {

    public static void doInsert(
            List<TileCentralDirectoryModel> batchList, LayerPerFileDao layerPerFileDao) {
        List<TileCentralDirectoryModel> insertList = new ArrayList<>(batchList);
        ForgeExecutorUtils.getExecutor()
                .submit(
                        () -> {
                            try {
                                layerPerFileDao.batchInsert(insertList);
                                insertList.clear();
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });
    }
}
