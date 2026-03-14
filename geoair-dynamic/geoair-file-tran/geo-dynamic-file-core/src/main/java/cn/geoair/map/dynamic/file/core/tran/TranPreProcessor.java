package cn.geoair.map.dynamic.file.core.tran;

import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;

/**
 * 转换预处理接口 用于转换前的校验、初始化、参数调整等
 */
@FunctionalInterface
public interface TranPreProcessor {

	/**
	 * 执行预处理
	 * @param reader 读取器
	 * @param writer 写入器
	 * @param context 转换上下文
	 * @return true-继续执行，false-终止转换
	 */
	boolean process(GeoFileReader reader, GeoFileWriter writer, TranContext context);

}
