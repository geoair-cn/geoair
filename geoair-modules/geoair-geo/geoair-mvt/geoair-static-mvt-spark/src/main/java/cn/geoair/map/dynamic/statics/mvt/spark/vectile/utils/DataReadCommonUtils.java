package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.List;

/** 数据读取通用工具类 抽离ID分页/BBox分片的通用逻辑 */
public class DataReadCommonUtils {

    /** 构建ID分页的页码列表 */
    public static List<Integer> buildPageNumberList(long totalCount, int maxPartionNum) {
        int countPerTask = (int) Math.round((float) totalCount / maxPartionNum);
        countPerTask = countPerTask <= 0 ? 1000 : countPerTask;

        List<Integer> pageNumbers = new ArrayList<>();
        int pageNumber = 0;
        long offset = 0;
        while (offset <= totalCount) {
            pageNumbers.add(pageNumber);
            offset += countPerTask;
            pageNumber++;
        }
        return pageNumbers;
    }

    /** 构建BBox分片条件列表 */
    public static List<String> buildBboxPartitionConditions(
            BBoxApo bBoxApo, int maxPartionNum, int sourceSrid) {
        double xmin = bBoxApo.getMinx();
        double xmax = bBoxApo.getMaxx();
        double ymin = bBoxApo.getMiny();
        double ymax = bBoxApo.getMaxy();

        // 外扩少量范围（避免分区边界要素遗漏）
        xmax = 1.0001 * xmax;
        ymax = 1.0001 * ymax;

        // 按平方根分区数切分空间范围（保证分区均匀）
        long numPerSide = Math.round(Math.sqrt(maxPartionNum));
        double stepX = (xmax - xmin) / numPerSide; // X方向步长
        double stepY = (ymax - ymin) / numPerSide; // Y方向步长

        // 构建分区条件列表（每个分区是"xmin,xmax,ymin,ymax"）
        List<String> partitionConditions = new ArrayList<>();
        for (int i = 0; i < numPerSide; i++) {
            for (int j = 0; j < numPerSide; j++) {
                double xminPartition = xmin + i * stepX;
                double xmaxPartition = xmin + (i + 1) * stepX;
                double yminPartition = ymin + j * stepY;
                double ymaxPartition = ymin + (j + 1) * stepY;
                partitionConditions.add(
                        String.format(
                                "%s,%s,%s,%s",
                                xminPartition, xmaxPartition, yminPartition, ymaxPartition));
            }
        }
        return partitionConditions;
    }

    /** 构建BBox查询SQL */
    public static String buildBboxQuerySql(
            String queryStatement,
            String geomFieldName,
            double xmin,
            double ymin,
            double xmax,
            double ymax,
            int sourceSrid) {
        String bufferBboxSqlFunction = getBufferBboxSqlFunction(xmin, ymin, xmax, ymax, sourceSrid);
        String sqlTemplate = "select * from {} as ttt where  ST_Intersects( ttt.{}, {})";
        return StrUtil.format(sqlTemplate, queryStatement, geomFieldName, bufferBboxSqlFunction);
    }

    /** 构建BBox SQL函数 */
    public static String getBufferBboxSqlFunction(
            double xmin, double ymin, double xmax, double ymax, int targetGrid) {
        return StrUtil.format(
                "public.ST_MakeEnvelope({}, {}, {}, {}, {})", xmin, ymin, xmax, ymax, targetGrid);
    }
}
