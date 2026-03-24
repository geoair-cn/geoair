package cn.geoair.map.dynamic.mvt.tools.model;

import java.util.List;
import java.util.Set;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import lombok.Data;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/6 16:02 @description： PBF瓦片生成专用参数对象
 */
@Data
public class PbfTileParameter {

	// 1. 几何简化级别
	private Integer simplificationLevel;

	// 2. 聚合距离（像素）
	private Integer coalesceDistance;

	// 3. 输出网格坐标系SRID
	private Integer outGridSrid;

	// 4. 是否生成标签图层
	private boolean createLabel;

	// 5. 是否生成边界图层
	private boolean createBoundary;

	// 是否仅仅生成一个pbf，用于节省内存
	private boolean isOnly = false;

	// 生成的pbf类型
	private PPbfType pPbfType = PPbfType.rootPbf;

	// 是否保存要素列表
	private boolean saveFeatureList = false;

	// 6. 图层名称（主图层）
	private String layerName;

	// 7. 标签图层名称
	private String layerNameLabel;

	// 8. 边界图层名称
	private String layerNameBoundary;

	// 9. 几何字段名（要素中存储几何的字段）
	private String geomFieldName;

	// 10. ID字段名（要素中存储唯一ID的字段）
	private String idFieldName;

	// 11. 需要保留的业务字段（白名单）
	private List<String> includeFields;

	// 12. 需要保留的系统字段（白名单）
	private Set<String> sysIncludeFields;

	/**
	 * 是否开启单瓦片要素数限制 对应命令行：--no-feature-limit
	 */
	private boolean enableFeatureLimitIs = false;

	/**
	 * 是否开启单瓦片大小限制 对应命令行：--no-tile-size-limit
	 */
	private boolean enableFeatureSizeLimit = false;

	/**
	 * 单瓦片最大要素数（默认tippecanoe内置值，仅enableFeatureLimit=false时生效） 对应命令行：-f/--feature-limit
	 */
	private Integer featureLimit;

	/**
	 * 单瓦片最大字节数 输入100KB，1MB这样可读的字符（默认tippecanoe内置值，仅enableTileSizeLimit=false时生效）
	 * 对应命令行：-S/--tile-size
	 */
	private String tileSizeLimit = "2MB";

	/**
	 * 是否按密度丢弃要素（优先丢弃高密度区域） 对应命令行：--drop-densest-as-needed
	 */
	private boolean dropDensestAsNeeded = true;

	/**
	 * 是否按密度合并要素（合并高密度区域相邻要素） 对应命令行：--coalesce-densest-as-needed
	 */
	private boolean coalesceDensestAsNeeded = true;

	public Long getTileSizeLimitByte() {
		if (tileSizeLimit == null) {
			return null;
		}
		return DataSizeUtil.parse(tileSizeLimit);
	}

	public PbfTileParameter copy() {
		PbfTileParameter copy = new PbfTileParameter();
		BeanUtil.copyProperties(this, copy);
		return copy;
	}

}
