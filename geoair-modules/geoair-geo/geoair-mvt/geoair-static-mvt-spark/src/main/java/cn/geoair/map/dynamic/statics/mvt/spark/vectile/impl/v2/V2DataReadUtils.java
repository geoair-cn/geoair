package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2;

import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.List;

/** V2 切片链路专用的数据读取辅助方法。 */
final class V2DataReadUtils {

    private V2DataReadUtils() {
    }

    static List<Integer> buildPageNumberList(long totalCount, int maxPartionNum) {
        int countPerTask = (int) Math.round((float) totalCount / maxPartionNum);
        countPerTask = countPerTask <= 0 ? 1000 : countPerTask;
        List<Integer> pageNumbers = new ArrayList<>();
        int pageNumber = 0;
        long offset = 0;
        while (offset <= totalCount) {
            pageNumbers.add(pageNumber++);
            offset += countPerTask;
        }
        return pageNumbers;
    }

    static List<String> buildBboxPartitionConditions(BBoxApo bBoxApo, int maxPartionNum, int sourceSrid) {
        double xmin = bBoxApo.getMinx();
        double xmax = 1.0001 * bBoxApo.getMaxx();
        double ymin = bBoxApo.getMiny();
        double ymax = 1.0001 * bBoxApo.getMaxy();
        long numPerSide = Math.round(Math.sqrt(maxPartionNum));
        double stepX = (xmax - xmin) / numPerSide;
        double stepY = (ymax - ymin) / numPerSide;
        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < numPerSide; i++) {
            for (int j = 0; j < numPerSide; j++) {
                conditions.add(String.format("%s,%s,%s,%s", xmin + i * stepX, xmin + (i + 1) * stepX, ymin + j * stepY, ymin + (j + 1) * stepY));
            }
        }
        return conditions;
    }

    static String buildBboxQuerySql(String queryStatement, String geomFieldName, double xmin, double ymin, double xmax, double ymax, int sourceSrid) {
        String bbox = StrUtil.format("public.ST_MakeEnvelope({}, {}, {}, {}, {})", xmin, ymin, xmax, ymax, sourceSrid);
        return StrUtil.format("select * from {} as ttt where  ST_Intersects( ttt.{}, {})", queryStatement, geomFieldName, bbox);
    }
}
