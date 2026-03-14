package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

import static cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt.不做任何操作;

public abstract class AbstractAdvSimplePagePreOpt extends AbstractAdvSimplePageOpt implements IAdvSimplePagePreOpt {

	protected static final GiLogger log = GirLogger.getLoger();

	public AbstractAdvSimplePagePreOpt(IDataSourceGetter dataSourceGetter) {
		super(dataSourceGetter);
	}

	protected abstract IAdvBaseOpt getAdvBaseOpt();

	protected abstract IAdvDDLOpt getAdvDDLOpt();

	protected abstract IAdvGeoPreOpt getAdvGeoPreOpt();

	// ========== 通用逻辑：带参数的总数统计 ==========
	@Override
	public Long pCount(String noPageSqlStatement, SqlParamMap sqlParam) {
		if (StrUtil.isEmpty(noPageSqlStatement)) {
			throw new IllegalArgumentException("分页统计SQL不能为空");
		}
		// 空参数初始化，避免NPE
		SqlParamMap param = sqlParam == null ? new SqlParamMap() : sqlParam;

		try {
			String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSqlStatement);
			String countSql = StrUtil.format("SELECT COUNT (1) AS count FROM ({}) AS {}", cleanSql,
					dialectTableNameProcessor.tbGetTempAliasTableName());
			// 子类实现：执行带参数的统计查询
			return executeCountSqlWithParam(countSql, param);
		}
		catch (Exception e) {
			log.error("带参数分页统计失败，SQL: {}, 参数: {}", noPageSqlStatement, sqlParam, e);
			throw new RuntimeException("带参数分页统计异常: " + e.getMessage(), e);
		}
	}

	// ========== 通用逻辑：核心带参数分页方法 ==========
	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {

		validateFullPageParams(noPageSqlStatement, pageNum, pageSize, pageNumStartZero, orders);
		SqlParamMap param = sqlParam == null ? new SqlParamMap() : sqlParam;

		// 2. 子类实现：带参数获取字段元数据
		DataFieldsApo dataFieldsApo = null;
		try {
			dataFieldsApo = getColumnsBySQLWithParam(noPageSqlStatement, param);
		}
		catch (Exception e) {
			log.error("查询带参数SQL字段元数据失败，SQL：{}，参数：{}", noPageSqlStatement, param, e);
			throw new RuntimeException("获取字段信息异常：" + e.getMessage(), e);
		}

		// 3. 通用：重构SQL（字段转义、临时表、排序）
		List<String> fieldNames = dataFieldsApo.getFieldList(FieldBySchemaApo::getColumnName, true);
		List<String> geomFieldNameList = dataFieldsApo.getGeomFieldNameList();
		String quotedFields = fieldNames.stream().map(this::quoteFieldName).collect(Collectors.joining(", "));
		String tableAlias = getTempTableAlias();
		String refactorNoPageSql = StrUtil.format("SELECT {} FROM ({}) AS {}", quotedFields,
				dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSqlStatement), tableAlias);
		String sqlWithOrder = pBuildSqlWithOrder(refactorNoPageSql, orders, tableAlias);

		// 4. 通用：带参数统计总数
		long total = pCount(sqlWithOrder, param);

		// 5. 通用：计算分页参数
		long offset = calculateOffset(pageNum, pageSize, pageNumStartZero);
		int lastPageNum = calculateLastPageNum(total, pageSize);

		// 6. 通用：构建分页SQL（子类实现语法）
		String pageSql = buildPageSql(sqlWithOrder, pageSize, offset);

		// 7. 子类实现：执行带参数的分页查询
		List<GirAdvOneRow> records = executePageSqlWithParam(pageSql, param, advEnumsGeomOpt, geomFieldNameList);

		// 8. 通用：构建分页结果
		PageApo<GirAdvOneRow> pageApo = createPageApo(total, pageNum, pageSize, pageNumStartZero, lastPageNum, offset,
				records);

		// 9. 通用：组装字段元数据
		if (hasFieldsInfo) {
			pageApo.setDataFieldsApo(dataFieldsApo);
		}

		return pageApo;
	}

	// ========== 通用：所有重载方法（统一调用核心方法） ==========
	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, 不做任何操作, false, ListUtil.empty());
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			AdvEnumsGeomOpt advEnumsGeomOpt) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, advEnumsGeomOpt, false, ListUtil.empty());
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			List<OrderApo> orders) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, 不做任何操作, false, orders);
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			boolean pageNumStartZero, boolean hasFieldsInfo) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, 不做任何操作, hasFieldsInfo,
				ListUtil.empty());
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo,
				ListUtil.empty());
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			boolean pageNumStartZero) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, 不做任何操作, false,
				ListUtil.empty());
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			boolean pageNumStartZero, List<OrderApo> orders) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, 不做任何操作, false, orders);
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, false,
				ListUtil.empty());
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, List<OrderApo> orders) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, false, orders);
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, advEnumsGeomOpt, hasFieldsInfo,
				ListUtil.empty());
	}

	@Override
	public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
			AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
		return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, advEnumsGeomOpt, hasFieldsInfo, orders);
	}

	/**
	 * 执行带参数的统计SQL，返回总数
	 */
	protected Long executeCountSqlWithParam(String countSql, SqlParamMap sqlParam) {
		GirAdvOneRow result = getAdvBaseOpt().bSelectOne(countSql, sqlParam);
		return result != null ? result.getLong("count") : 0L;
	}

	@Override
	protected Long executeCountSql(String countSql) {
		return executeCountSqlWithParam(countSql, new SqlParamMap());
	}

	/**
	 * 带参数获取SQL字段元数据
	 */

	protected DataFieldsApo getColumnsBySQLWithParam(String noPageSql, SqlParamMap sqlParam) {
		return getAdvDDLOpt().dGetColumnsBySQL(noPageSql, sqlParam);
	}

	@Override
	protected DataFieldsApo getColumnsBySQL(String noPageSql) {
		return getColumnsBySQLWithParam(noPageSql, new SqlParamMap());
	}

	/**
	 * 执行带参数的分页查询，返回结果列表
	 */
	protected List<GirAdvOneRow> executePageSqlWithParam(String pageSql, SqlParamMap sqlParam,
			AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
		return getAdvGeoPreOpt().eSelectList(pageSql, sqlParam, advEnumsGeomOpt, geomFieldNameList);
	}

	@Override
	protected List<GirAdvOneRow> executePageSql(String pageSql, AdvEnumsGeomOpt advEnumsGeomOpt,
			List<String> geomFieldNameList) {
		return executePageSqlWithParam(pageSql, new SqlParamMap(), advEnumsGeomOpt, geomFieldNameList);
	}

}
