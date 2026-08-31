package cn.geoair.map.dynamic.file.core.write;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;

import org.geotools.api.feature.simple.SimpleFeatureType;

import java.io.Closeable;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/9 14:55 @description： 写入器
 */
public interface GeoFileWriter extends Closeable {

    // 链接信息
    void setLinkInfo(LinkInfo linkInfo);

    // 设置写入配置信息
    void setWriteConfig(WriteConfig writeConfig);

    // 写入表头
    GeoFileWriter writeHeader(SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer);

    // 写入一行
    GeoFileWriter writeOneRow(GirAdvOneRow girAdvOneRow, ExceptionConsumer exceptionConsumer);

    // 批量插入
    default GeoFileWriter writeRows(List<GirAdvOneRow> rows, ExceptionConsumer exceptionConsumer) {
        rows.forEach(row -> writeOneRow(row, exceptionConsumer));
        return this;
    }
}
