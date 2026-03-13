package cn.geoair.comp.knife4j.ext.core.config;

import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:13 @description： 父类接口
 */
public interface GirSwaggerApiConfig {

	List<DocketInfo> getDocketInfos();

	ApiModelInfo getApiModelInfo();

}
