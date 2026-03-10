package cn.geoair.map.dynamic.dbservice.model.dbapi.seo;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.map.dynamic.dbservice.model.dbapi.entity.DbApiDataSourcePo;
import cn.hutool.core.bean.BeanUtil;

import java.util.Date;

/**
 * 数据源信息(DbapiDatasource)Seo
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "数据源信息SearchDto")
public class DbApiDataSourceSeo extends DbApiDataSourcePo {

	private static final long serialVersionUID = 1753953250862L;

	public static DbApiDataSourceSeo emptySeo() {
		return new DbApiDataSourceSeo();
	}

	public DbApiDataSourceSeo copy() {
		DbApiDataSourceSeo copy = new DbApiDataSourceSeo();
		BeanUtil.copyProperties(this, copy);
		return copy;
	}

	@GaModelField(text = "模糊查询")
	private String[] andQueryContentIn;

	@GaModelField(text = "查询多个主键数据")
	private String[] andIdsIn;

	@GaModelField(text = "查询排除多个主键数据")
	private String[] andIdsNotIn;

	@GaModelField(text = "创建时间始")
	private Date timeCreateStart;

	@GaModelField(text = "创建时间止")
	private Date timeCreateEnd;

	@GaModelField(text = "更新时间始")
	private Date timeUpdateStart;

	@GaModelField(text = "更新时间止")
	private Date timeUpdateEnd;

	public String[] getAndQueryContentIn() {
		return andQueryContentIn;
	}

	public void setAndQueryContentIn(String[] andQueryContentIn) {
		this.andQueryContentIn = andQueryContentIn;
	}

	public void setAndIdsIn(String[] andIdsIn) {
		this.andIdsIn = andIdsIn;
	}

	public String[] getAndIdsIn() {
		return this.andIdsIn;
	}

	public void setAndIdsNotIn(String[] andIdsNotIn) {
		this.andIdsNotIn = andIdsNotIn;
	}

	public String[] getAndIdsNotIn() {
		return andIdsNotIn;
	}

	public void setTimeCreateStart(Date timeCreateStart) {
		this.timeCreateStart = timeCreateStart;
	}

	public Date getTimeCreateStart() {
		return timeCreateStart;
	}

	public void setTimeCreateEnd(Date timeCreateEnd) {
		this.timeCreateEnd = timeCreateEnd;
	}

	public Date getTimeCreateEnd() {
		return timeCreateEnd;
	}

	public void setTimeUpdateStart(Date timeUpdateStart) {
		this.timeUpdateStart = timeUpdateStart;
	}

	public Date getTimeUpdateStart() {
		return timeUpdateStart;
	}

	public void setTimeUpdateEnd(Date timeUpdateEnd) {
		this.timeUpdateEnd = timeUpdateEnd;
	}

	public Date getTimeUpdateEnd() {
		return timeUpdateEnd;
	}

}
