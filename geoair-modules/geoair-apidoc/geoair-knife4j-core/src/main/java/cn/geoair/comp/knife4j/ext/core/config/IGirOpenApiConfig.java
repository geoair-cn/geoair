package cn.geoair.comp.knife4j.ext.core.config;

import java.util.List;

import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:13 @description： 父类接口
 */
public interface IGirOpenApiConfig {

	/**
	 * 获取Docket信息
	 * @return
	 */
	List<DocketInfo> getDocketInfos();

	/**
	 * 获取ApiModel信息
	 * @return
	 */
	ApiModelInfo getApiModelInfo();

	/**
	 * 是否加载OpenApi配置完成
	 */
	boolean isLoad();

	/**
	 * 开始加载
	 */
	void doLoading();

	/**
	 * 加载完成
	 */
	void loadEnd();

}
