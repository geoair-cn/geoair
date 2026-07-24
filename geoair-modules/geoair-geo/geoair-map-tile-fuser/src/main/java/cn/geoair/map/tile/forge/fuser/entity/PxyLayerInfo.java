package cn.geoair.map.tile.forge.fuser.entity;

import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.fuser.enums.SrcType;

import jakarta.persistence.Transient;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 代理图层信息实体类
 *
 * <p>用于配置瓦片图层的代理获取参数，支持网络代理和本地文件两种方式
 *
 * @author 张俊
 * @date Created in 2026/6/15 11:00
 * @description 代理图层配置信息
 */
@Data
@Accessors(chain = true)
public class PxyLayerInfo {

    /**
     * 图层名称
     *
     * <p>用于标识和区分不同的瓦片图层，在缓存中作为目录名使用
     *
     * <p>示例：img_w、vec_w、terrain等
     */
    @GaModelField(text = "图层名称")
    private String layerName;

    /**
     * 资源路径模板
     *
     * <p>当type为local时，表示本地文件路径模板，支持{z}、{x}、{y}占位符
     *
     * <p>当type为web时，表示网络URL模板，支持{z}、{x}、{y}占位符
     *
     * <p>示例：本地模板 - D:/tiles/{z}/{x}/{y}.png
     *
     * <p>示例：网络模板 - http://tile.example.com/{z}/{x}/{y}.png
     */
    @GaModelField(text = "资源路径模板")
    private String path;

    /** 坐标原点类型，默认谷歌原点 */
    @GaModelField(text = "坐标原点类型，默认谷歌原点", em = OriginType.class)
    private String originType;

    /**
     * 获取器类型
     *
     * <p>指定瓦片数据的来源方式
     *
     * <ul>
     *   <li>web - 通过网络HTTP请求获取瓦片
     *   <li>local - 从本地文件系统读取瓦片
     *   <li>database - 从数据库获取（预留）
     * </ul>
     *
     * @see SrcType
     */
    @GaModelField(text = "获取器类型", em = SrcType.class)
    private String srcType;

    /** 图片类型 默认png */
    private String imageType = "png";

    /** 网格坐标系，默认3857 */
    private Integer gridSrid = 3857;

    /**
     * 是否启用缓存逻辑
     *
     * <ul>
     *   <li>true 或 1 - 启用缓存
     *   <li>false 或 0 - 不启用缓存
     * </ul>
     */
    private String enableCache;

    /**
     * 是否使用网络代理
     *
     * <p>仅当type为web时有效，控制是否通过代理服务器访问瓦片服务
     *
     * <ul>
     *   <li>true 或 1 - 启用代理
     *   <li>false 或 0 - 不启用代理（直连）
     * </ul>
     */
    private String useWebPxy;

    /**
     * 代理服务器主机地址
     *
     * <p>仅当useWebPxy为true时有效，指定代理服务器的IP地址或域名
     *
     * <p>示例：127.0.0.1、proxy.example.com
     */
    private String webPxyHost;

    /**
     * 代理服务器端口号
     *
     * <p>仅当useWebPxy为true时有效，指定代理服务器的端口
     *
     * <p>常见端口：HTTP代理-8080、SOCKS代理-1080
     *
     * <p>示例：8080、3128、1080
     */
    private Integer webPxyPort;

    @Transient
    public OriginType getOriginTypeEnums() {
        return OriginType.fromMode(originType);
    }

    @Transient
    public SrcType getSrcTypeEnums() {
        return SrcType.fromCode(srcType);
    }

    /**
     * 是否为3857网格
     *
     * @return
     */
    @Transient
    public boolean isGoogleGrid() {
        return gridSrid == 3857;
    }

    /** 检查缓存是否启用 */
    @Transient
    public boolean isCacheEnabled(PxyLayerInfo config) {
        return "true".equalsIgnoreCase(config.getEnableCache())
                || "1".equals(config.getEnableCache());
    }
}
