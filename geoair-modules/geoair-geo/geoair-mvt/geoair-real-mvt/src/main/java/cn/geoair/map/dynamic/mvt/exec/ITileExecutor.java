package cn.geoair.map.dynamic.mvt.exec;

import com.alibaba.fastjson2.JSONObject;

import cn.geoair.map.dynamic.mvt.dto.TileExecutorConfig;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.exec.dto.TileRequest;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/12/19 11:05 @description： 实时矢量瓦片执行器
 */
public interface ITileExecutor {

	/**
	 * 获取瓦片执行器配置
	 * @return
	 */
	TileExecutorConfig getTileExecutorConfig();

	/**
	 * 设置需要传递的变量，不做任何处理
	 * @param customVariable
	 * @return
	 */
	ITileExecutor setCustomVariable(JSONObject customVariable);

	/**
	 * 获取瓦片服务相关参数
	 * @return
	 */
	TileGlobalConfig getTileGlobalConfig();

	/**
	 * 实时矢量瓦片执行的时候一部分配置信息
	 * @return
	 */
	TileExecParams getTileExecParams(int zoom, int x, int y);

	/**
	 * 设置瓦片执行器配置
	 * @param tileExecutorConfig 瓦片执行器配置对象
	 */
	void setTileExecutorConfig(TileExecutorConfig tileExecutorConfig);

	/**
	 * 获取指定坐标的瓦片数据
	 * @param zoom 瓦片级别
	 * @param x 瓦片列号
	 * @param y 瓦片行号
	 * @return 瓦片请求数据
	 */
	TileRequest getTileData(int zoom, int x, int y);

	/**
	 * 获取最终执行的sql
	 * @param tileExecParams
	 * @return
	 */
	String getExecSql(TileExecParams tileExecParams);

}
