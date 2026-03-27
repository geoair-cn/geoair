package cn.geoair.map.dynamic.file.core.tran;

import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;

/** 转换后处理接口 用于转换完成后的结果校验、资源清理、数据归档等 */
@FunctionalInterface
public interface TranPostProcessor {

    /**
     * 执行后处理
     *
     * @param result 转换结果
     * @param context 转换上下文
     */
    void process(TranResult result, TranContext context);
}
