package cn.geoair.comp.dynamic.ds;

import cn.geoair.comp.dynamic.ds.base.IDsConnectionOpt;
import cn.geoair.comp.dynamic.ds.base.IDsDataSourceOpt;
import cn.geoair.comp.dynamic.ds.tx.IDsTransactionTemplate;

/**
 * 数据源获取器接口
 *
 * <p>该接口定义了数据源的初始化、获取和管理方法，提供了多种初始化方式和资源管理功能。 实现该接口的类可以通过不同的方式初始化数据源，并提供获取数据库连接、关闭资源等操作。
 *
 * @author zhangjun
 * @date Created in 2025/10/9 10:38
 */
public interface IDataSourceGetter
        extends IDsDataSourceOpt, IDsConnectionOpt, IDsTransactionTemplate {}
