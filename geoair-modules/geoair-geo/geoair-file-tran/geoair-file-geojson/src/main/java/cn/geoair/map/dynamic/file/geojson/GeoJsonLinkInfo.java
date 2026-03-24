package cn.geoair.map.dynamic.file.geojson;

import java.io.File;

import cn.geoair.map.dynamic.file.core.link.LinkInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * GeoJSON 文件链接信息
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class GeoJsonLinkInfo extends LinkInfo {

	/**
	 * GeoJSON 文件路径（.geojson 或 .json 文件）
	 */
	private String geoJsonFilePath;

	/**
	 * 文件编码（默认 UTF-8）
	 */
	private String charset = "UTF-8";

	/**
	 * geojson的srid
	 */
	private int srid = 4326;

	/**
	 * 检查链接是否可用（读取时校验存在性，写入时校验父目录）
	 */
	@Override
	public void checkLinkInfo() {
		if (geoJsonFilePath == null || geoJsonFilePath.trim().isEmpty()) {
			throw new IllegalArgumentException("GeoJSON 文件路径不能为空");
		}

		File geoJsonFile = new File(geoJsonFilePath);
		// 读取场景：校验文件存在；写入场景：校验父目录可写
		if (geoJsonFile.exists()) {
			if (!geoJsonFile.isFile()) {
				throw new IllegalArgumentException("指定路径不是文件：" + geoJsonFilePath);
			}
		}
		else {
			File parentDir = geoJsonFile.getParentFile();
			if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
				throw new IllegalArgumentException("无法创建父目录：" + parentDir.getAbsolutePath());
			}
		}

		// 简单校验文件后缀
		String fileName = geoJsonFile.getName().toLowerCase();
		if (!fileName.endsWith(".geojson") && !fileName.endsWith(".json")) {
			throw new IllegalArgumentException("文件不是合法的 GeoJSON 文件（建议后缀 .geojson 或 .json）：" + geoJsonFilePath);
		}
	}

}
