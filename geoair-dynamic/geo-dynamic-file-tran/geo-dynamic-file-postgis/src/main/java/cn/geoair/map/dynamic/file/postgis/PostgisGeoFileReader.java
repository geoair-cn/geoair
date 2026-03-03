package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.utils.AdvJdbcUrlUtil;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.*;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.*;

/**
 * 改造后的 PostGIS 读取器 适配新接口：readHeader 返回 SimpleFeatureType，补全核心读取逻辑 基于 GeoTools 实现，统一要素类型标准
 */
@Slf4j
public class PostgisGeoFileReader implements GeoFileReader {

	private PostgisReadLinkInfo linkInfo;

	private DataStore postgisDataStore;

	private SimpleFeatureType featureType; // 核心：返回的要素类型

	private FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

	private FeatureIterator<SimpleFeature> featureIterator;

	private long totalCount; // 总记录数

	@Override
	public void setLinkInfo(LinkInfo linkInfo) {
		if (!(linkInfo instanceof PostgisReadLinkInfo)) {
			throw new IllegalArgumentException("链接信息必须是 PostgisReadLinkInfo 类型");
		}
		this.linkInfo = (PostgisReadLinkInfo) linkInfo;
		this.linkInfo.checkLinkInfo();
		initPostgisDataStore();
		initFeatureSource();
		initFeatureType();
		calculateTotalCount();
	}

	/**
	 * 初始化 GeoTools PostGIS DataStore（替代原生 JDBC 连接）
	 */
	private void initPostgisDataStore() {
		try {
			Map<String, Object> params = new HashMap<>();
			params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
			params.put(PostgisNGDataStoreFactory.HOST.key, extractHostFromJdbcUrl(linkInfo.getJdbcUrl()));
			params.put(PostgisNGDataStoreFactory.PORT.key, extractPortFromJdbcUrl(linkInfo.getJdbcUrl()));
			params.put(PostgisNGDataStoreFactory.DATABASE.key, extractDbNameFromJdbcUrl(linkInfo.getJdbcUrl()));
			params.put(PostgisNGDataStoreFactory.USER.key, linkInfo.getUsername());
			params.put(PostgisNGDataStoreFactory.PASSWD.key, linkInfo.getPassword());
			params.put(PostgisNGDataStoreFactory.SCHEMA.key, linkInfo.getSchema());
			params.put(PostgisNGDataStoreFactory.PREPARED_STATEMENTS.key, true);

			this.postgisDataStore = DataStoreFinder.getDataStore(params);
			if (postgisDataStore == null) {
				throw new RuntimeException("初始化 GeoTools PostGIS DataStore 失败");
			}
			log.info("PostGIS DataStore 初始化成功，表名：{}", linkInfo.getQuerySqlByOutPut());
		}
		catch (Exception e) {
			throw new RuntimeException("初始化 PostGIS 连接失败", e);
		}
	}

	/**
	 * 初始化要素源（FeatureSource），获取表结构
	 */
	private void initFeatureSource() {
		try {
			String viewName = IdUtil.fastSimpleUUID();
			VirtualTable virtualTable = new VirtualTable(viewName, linkInfo.getQuerySqlByOutPut());
			((JDBCDataStore) postgisDataStore).createVirtualTable(virtualTable);
			this.featureSource = postgisDataStore.getFeatureSource(viewName);
			this.featureIterator = featureSource.getFeatures().features();
		}
		catch (Exception e) {
			throw new RuntimeException("初始化 PostGIS 要素源失败", e);
		}
	}

	private void initFeatureType() {
		try {
			Query query = new Query();
			query.setMaxFeatures(1);
			FeatureIterator<SimpleFeature> features = featureSource.getFeatures(query).features();
			if (features.hasNext()) {
				SimpleFeature feature = features.next();
				features.close();
				featureType = feature.getFeatureType();
			}
		}
		catch (Exception e) {
			throw new RuntimeException("初始化 PostGIS 要素源失败", e);
		}
	}

	/**
	 * 计算总记录数（GeoTools 原生方法）
	 */
	private void calculateTotalCount() {
		try {
			Query countQuery = new Query(linkInfo.getQuerySqlByOutPut());
			this.totalCount = featureSource.getCount(countQuery);
			log.info("PostGIS 表 {} 总记录数：{}", linkInfo.getQuerySqlByOutPut(), totalCount);
		}
		catch (IOException e) {
			log.warn("计算 PostGIS 表总记录数失败，将返回 0", e);
			this.totalCount = 0;
		}
	}

	/**
	 * 改造核心：返回 SimpleFeatureType（替代原 List<Pair>）
	 */
	@Override
	public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
		try {
			// 可选：补充几何字段的 SRID（确保和配置一致）
			if (linkInfo.getSrid() > 0) {
				SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
				typeBuilder.init(featureType);
				CoordinateReferenceSystem crs = GirAdvTools.getSridOpt().getCRS(linkInfo.getSrid());
				typeBuilder.setCRS(crs);
				this.featureType = typeBuilder.buildFeatureType();
			}
			return this.featureType;
		}
		catch (Exception e) {
			if (exceptionConsumer != null) {
				exceptionConsumer.accept(e);
				return null;
			}
			else {
				throw new RuntimeException("读取 PostGIS 表头（SimpleFeatureType）失败", e);
			}
		}
	}

	/**
	 * 补全：读取单行数据（适配 GirAdvOneRow）
	 */
	@Override
	public GirAdvOneRow readOneRow(ExceptionConsumer exceptionConsumer) {
		try {
			if (featureIterator == null || !featureIterator.hasNext()) {
				return null;
			}
			SimpleFeature feature = featureIterator.next();
			Map<String, Object> attributes = new HashMap<>();

			// 遍历要素属性（自动解析几何字段）
			for (Property property : feature.getProperties()) {
				String propName = property.getName().getLocalPart();
				Object propValue = property.getValue();

				// 几何字段转换为 JTS Geometry（GeoTools 原生支持）
				if (propValue instanceof Geometry) {
					attributes.put(propName, propValue);
				}
				else {
					attributes.put(propName, propValue);
				}
			}

			// 转换为 GirAdvOneRow
			return GirAdvOneRow.ofByMap(attributes);
		}
		catch (Exception e) {
			if (exceptionConsumer != null) {
				exceptionConsumer.accept(e);
				return null;
			}
			else {
				throw new RuntimeException("读取 PostGIS 单行数据失败", e);
			}
		}
	}

	/**
	 * 补全：读取分页数据（支持排序）
	 */
	@Override
	public GirPager<GirAdvOneRow> readRowPage(GirPageParam girPageParam, ExceptionConsumer exceptionConsumer) {
		GirPager<GirAdvOneRow> pager = new GirPager<>();
		try {
			if (girPageParam == null) {
				throw new IllegalArgumentException("分页参数不能为空");
			}

			int pageNum = girPageParam.getPageNum();
			int pageSize = girPageParam.getPageSize();
			int startIndex = (pageNum - 1) * pageSize;

			// 构建分页查询（GeoTools 原生分页）
			Query pageQuery = new Query(linkInfo.getQuerySqlByOutPut());
			pageQuery.setStartIndex(startIndex);
			pageQuery.setMaxFeatures(pageSize);

			// 执行分页查询
			FeatureCollection<SimpleFeatureType, SimpleFeature> pageFeatures = featureSource.getFeatures(pageQuery);
			List<GirAdvOneRow> rowList = new ArrayList<>();

			try (FeatureIterator<SimpleFeature> pageIterator = pageFeatures.features()) {
				while (pageIterator.hasNext()) {
					SimpleFeature feature = pageIterator.next();
					Map<String, Object> attributes = new HashMap<>();

					for (Property property : feature.getProperties()) {
						String propName = property.getName().getLocalPart();
						Object propValue = property.getValue();
						attributes.put(propName, propValue);
					}

					rowList.add(GirAdvOneRow.ofByMap(attributes));
				}
			}

			// 设置分页结果
			pager.put(rowList, totalCount, girPageParam);
			log.info("读取 PostGIS 分页数据：第{}页，共{}条，总页数{}", pageNum, rowList.size(), pager.getTotal());

		}
		catch (Exception e) {
			if (exceptionConsumer != null) {
				exceptionConsumer.accept(e);
			}
			else {
				throw new RuntimeException("读取 PostGIS 分页数据失败", e);
			}
		}
		return pager;
	}

	/**
	 * 关闭资源（GeoTools 原生资源释放）
	 */
	@Override
	public void close() {
		try {
			if (featureIterator != null) {
				featureIterator.close();
			}
			if (postgisDataStore != null) {
				postgisDataStore.dispose();
			}
			log.info("PostGIS 读取器资源已释放");
		}
		catch (Exception e) {
			log.error("关闭 PostGIS 读取器资源失败", e);
		}
	}

	private String extractHostFromJdbcUrl(String jdbcUrl) {
		return AdvJdbcUrlUtil.splitter(jdbcUrl).host;
	}

	private Integer extractPortFromJdbcUrl(String jdbcUrl) {
		return Integer.parseInt(AdvJdbcUrlUtil.splitter(jdbcUrl).port);
	}

	private String extractDbNameFromJdbcUrl(String jdbcUrl) {
		return AdvJdbcUrlUtil.splitter(jdbcUrl).database;
	}

}
