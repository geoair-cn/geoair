package cn.geoair.map.dynamic.file.core.tran;

import java.io.Closeable;

import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;

/**
 * 空间文件转换核心接口
 *
 * @author 张逢吉
 * @date 2022/2/9 14:52
 */
public interface GeoFileTran extends Closeable {

	public static GeoFileTran getInstance() {
		return new GeoFileTranImpl();
	}

	/**
	 * 核心转换方法（全量转换）
	 * @param reader 输入读取器
	 * @param writer 输出写入器
	 * @return 结构化转换结果（包含统计、状态、异常等）
	 */
	TranResult transform(GeoFileReader reader, GeoFileWriter writer);

	/**
	 * 核心转换方法（带自定义上下文）
	 * @param reader 输入读取器
	 * @param writer 输出写入器
	 * @param context 转换上下文（传递自定义参数、配置）
	 * @return 结构化转换结果
	 */
	TranResult transform(GeoFileReader reader, GeoFileWriter writer, TranContext context);

	/**
	 * 设置全局异常处理器
	 * @param exceptionConsumer 异常处理器
	 * @return 当前转换实例（链式调用）
	 */
	GeoFileTran setExceptionConsumer(ExceptionConsumer exceptionConsumer);

	/**
	 * 设置转换进度监听器
	 * @param progressListener 进度监听器
	 * @return 当前转换实例（链式调用）
	 */
	GeoFileTran setProgressListener(TranProgressListener progressListener);

	/**
	 * 获取转换上下文（支持中途修改配置）
	 * @return 转换上下文
	 */
	TranContext getContext();

	/**
	 * 重置转换状态（复用实例）
	 */
	void reset();

}
