package cn.geoair.comp.knife4j.ext.core.config;

import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;

import java.util.List;

/**
 * IGirOpenApiConfig interface.
 *
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:13 @description： 父类接口
 * @version $Id: $Id
 */
public interface IGirOpenApiConfig {

    /**
     * 获取Docket信息
     *
     * @return a {@link java.util.List} object
     */
    List<DocketInfo> getDocketInfos();

    /**
     * 获取ApiModel信息
     *
     * @return a {@link cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo} object
     */
    ApiModelInfo getApiModelInfo();

    /**
     * 是否加载OpenApi配置完成
     *
     * @return a boolean
     */
    boolean isLoad();

    /** 开始加载 */
    void doLoading();

    /** 加载完成 */
    void loadEnd();
}
