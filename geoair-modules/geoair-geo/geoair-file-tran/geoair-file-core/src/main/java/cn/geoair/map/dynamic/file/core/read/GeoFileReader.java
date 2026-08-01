package cn.geoair.map.dynamic.file.core.read;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;

import java.io.Closeable;
import java.util.Iterator;
import org.geotools.api.feature.simple.SimpleFeatureType;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/9 14:55 @description： 读取器
 */
public interface GeoFileReader extends Closeable {

    // 链接信息
    void setLinkInfo(LinkInfo linkInfo);

    long getFeatureCount();

    // 读取表头
    SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer);

    // 读取一行
    GirAdvOneRow readNextRow(ExceptionConsumer exceptionConsumer);

    Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer);

    // 读取分页行数
    GirPager<GirAdvOneRow> readRowPage(
            GirPageParam girPageParam, ExceptionConsumer exceptionConsumer);
}
